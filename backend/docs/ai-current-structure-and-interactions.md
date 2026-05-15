# AI Current Structure And Interactions

Tai lieu nay mo ta package `backend/src/main/java/com/socialpulse/app/ai` theo code hien tai, khong dua tren muc tieu tach module trong tuong lai.

Pham vi doc chinh:

- `backend/src/main/java/com/socialpulse/app/ai`
- `backend/src/main/java/com/socialpulse/app/feed`
- `backend/src/main/resources/ai`
- `backend/src/test/java/com/socialpulse/app/ai`

## 1. Ket luan nhanh

`ai` hien tai chua la mot module offline doc lap. No dang la mot package ben trong backend monolith, gom ca:

- offline training pipeline tu Pushshift `.zst`
- shared schema va scorer cho model dump
- runtime service de backend load artifact JSON va score feed

Noi cach khac:

- training code va inference code dang song chung trong cung source set Maven
- backend `feed` goi truc tiep vao `ai`
- `ai` cung phu thuoc nguoc lai vao DTO va use case cua `feed`

Vi vay, ranh gioi hien tai la "package separation", chua phai "module separation".

## 2. Cay package hien tai

```text
com.socialpulse.app.ai
|- inference
|  |- LightGbmFeatureVectorizer
|  |- LightGbmRankingService
|  `- config
|     `- LightGbmProperties
|- shared
|  |- LightGbmFeatureSchema
|  |- LightGbmModel
|  |- LightGbmModelArtifact
|  `- LightGbmModelScorer
`- training
   |- GradientBoostedTreeTrainer
   |- PushshiftDatasetScanner
   |- PushshiftFeatureEngineering
   |- PushshiftTrainingCli
   |- PushshiftTrainingPipeline
   |- TrainingArguments
   |- TrainingJsonSupport
   `- TrainingTypes
```

## 3. Vai tro tung package

### 3.1. `inference/config`

`LightGbmProperties` la config runtime cho backend inference:

- `ai.lightgbm.enabled`
- `ai.lightgbm.model-location`
- `ai.lightgbm.feature-schema-version`

Class nay khong dung cho training pipeline. No chi phuc vu backend khi load model artifact va nam trong boundary inference.

### 3.2. `shared`

Day la lop "shared AI core" cua he thong hien tai.

- `LightGbmFeatureSchema`
  - la source of truth cho `DEFAULT_SCHEMA_VERSION`
  - khai bao `FEATURE_ORDER`
  - khai bao default preprocessing values nhu `DEFAULT_UPVOTE_RATIO`
- `LightGbmModel`
  - object model de doc JSON dump
- `LightGbmModelArtifact`
  - object model cho wrapped artifact co metadata + `model_dump`
- `LightGbmModelScorer`
  - scorer local, duyet tree dump va tinh score trong Java

Luu y quan trong:

- `shared` la boundary dung cho contract va scorer dung chung
- nhung training implementation hien tai khong dung thu vien LightGBM that
- scorer chi can artifact JSON co shape tuong thich voi `LightGbmModel`

### 3.3. `inference`

Day la lop runtime adapter giua feed ranking va AI scorer.

- `LightGbmFeatureVectorizer`
  - chuyen `RankingFeatures` cua backend thanh `Map<String, Double>`
  - dam bao ten feature trung voi training contract
- `LightGbmRankingService`
  - bridge giua AI va feed ranking runtime

`LightGbmRankingService` la bridge giua AI va feed ranking runtime.

Trach nhiem:

- nhan `RankingRequest`
- check `enabled`
- check schema version
- load JSON artifact tu `ResourceLoader`
- parse raw model dump hoac wrapped artifact
- tao `LightGbmModelScorer`
- score tung `RankingFeatures`
- tra `RankingResponse`

No implement truc tiep `PredictRankingUseCase`, nen boundary inference van chua doc lap voi `feed`.

### 3.4. `training`

Day la offline pipeline chay bang CLI trong cung backend source set.

- `PushshiftTrainingCli`
  - entry point
  - parse args
  - in ket qua JSON ra stdout
- `TrainingArguments`
  - parse va validate command arguments
- `TrainingJsonSupport`
  - doc `.zst` JSONL
  - write artifact JSON
- `PushshiftDatasetScanner`
  - stream submissions/comments tu Pushshift
  - loc du lieu
  - reservoir sampling submissions
  - tinh author aggregates
- `PushshiftFeatureEngineering`
  - bien `SubmissionRecord` thanh `TrainingRow`
  - hash split train/validation
- `GradientBoostedTreeTrainer`
  - train custom boosted tree regressor
  - export model dump co format tuong thich scorer
- `PushshiftTrainingPipeline`
  - orchestration toan bo flow
  - wrap metadata thanh artifact cuoi

## 4. Luong du lieu hien tai

### 4.1. Offline training

```text
Pushshift .zst
-> PushshiftDatasetScanner
-> PushshiftFeatureEngineering
-> GradientBoostedTreeTrainer
-> JSON artifact
```

Chi tiet:

1. `PushshiftTrainingCli` nhan:
   - `--submissions`
   - `--comments`
   - `--output`
   - cac hyperparameter khac
2. `PushshiftDatasetScanner` doc streaming tu `.zst`, loc record khong hop le.
3. Scanner xay `SubmissionRecord` voi cac field nhu:
   - `titleLength`
   - `bodyLength`
   - `score`
   - `numComments`
   - `numCrossposts`
   - `hasMultimedia`
   - `isSharePost`
   - `hotScore`
4. `PushshiftFeatureEngineering` bien record thanh vector theo `LightGbmFeatureSchema.FEATURE_ORDER`.
5. `GradientBoostedTreeTrainer` train mot model regression tren label `log1p(popularity)`.
6. `PushshiftTrainingPipeline` wrap artifact:
   - `artifact_version`
   - `feature_schema_version`
   - `training_dataset`
   - `trained_at`
   - `label_strategy`
   - `training_summary`
   - `model_dump`

### 4.2. Online inference trong backend

```text
Candidate posts
-> FeatureExtractionService
-> LightGbmFeatureVectorizer
-> LightGbmRankingService
-> LightGbmModelScorer
-> RankingResponse
-> FeedRankingService sort/fallback
```

Chi tiet:

1. `FeedRankingService` lay candidate posts.
2. `FeatureExtractionService` build `RankingFeatures` tu:
   - post data
   - author data
   - viewer data
   - follow relation
   - Redis cache
3. `FeedRankingService` tao `RankingRequest` kem `featureSchemaVersion`.
4. `LightGbmRankingService` load artifact tu file/resource.
5. `LightGbmFeatureVectorizer` doi `RankingFeatures` sang map feature-name -> value.
6. `LightGbmModelScorer` score tung post.
7. `FeedRankingService` validate prediction set:
   - du so luong
   - dung `postId`
   - score finite
   - dung `featureSchemaVersion`
8. Neu prediction invalid hoac model unavailable, backend fallback sang ranking deterministic.

## 5. Dependency map

## 5.1. Backend goi vao `ai`

`feed` phu thuoc vao `ai` o nhieu diem:

- `FeedConfig`
  - enable `LightGbmProperties`
  - tao bean `LightGbmFeatureVectorizer`
  - tao bean `LightGbmRankingService`
  - expose `PredictRankingUseCase`
- `RankingRequest`
  - lay default schema version tu `LightGbmFeatureSchema`

Dependency huong nay la hop ly cho runtime scoring.

## 5.2. `ai` goi nguoc lai vao `feed`

Day la coupling chat nhat trong code hien tai:

- `LightGbmFeatureVectorizer` import:
  - `RankingFeatures`
  - `PostFeatures`
  - `UserFeatures`
  - `InteractionFeatures`
- `LightGbmRankingService` import:
  - `RankingRequest`
  - `RankingResponse`
  - `PredictRankingUseCase`

Y nghia:

- `ai` khong chi cung cap scorer
- no dang biet truc tiep data contract va use-case contract cua `feed`

Neu tach `ai` thanh module rieng, day la diem can xu ly dau tien.

## 5.3. `training` phu thuoc gi?

`training` khong phu thuoc truc tiep vao `feed`, `post`, `user`, `follow`, `auth`.
No chu yeu phu thuoc vao:

- `ai.shared.LightGbmFeatureSchema`
- Jackson
- zstd-jni
- Java standard library

Vi vay, phan offline training da "gan doc lap" hon inference layer.

## 6. Contract chung giua training va inference

Contract quan trong nhat la `LightGbmFeatureSchema`.

No quyet dinh:

- feature order
- feature names
- default numeric values
- schema version

Training dang dung contract nay o:

- `PushshiftFeatureEngineering`
- `GradientBoostedTreeTrainer`
- `PushshiftTrainingPipeline`

Inference dang dung contract nay o:

- `LightGbmFeatureVectorizer`
- `LightGbmProperties`
- `RankingRequest`

Neu thay doi feature ma khong bump schema version, backend co nguy co:

- load nham artifact
- score sai do map feature khong con khop
- cho ra ket qua hop le ve ky thuat nhung sai ve nghia

## 7. Muc do "LightGBM" hien tai

Can phan biet ro 2 viec:

1. Runtime scorer dang doc dump theo shape tuong thich LightGBM.
2. Training code hien tai la custom implementation, khong goi LightGBM library.

Cu the:

- `GradientBoostedTreeTrainer` tu xay boosted tree bang residual fitting
- objective hien tai la `regression`
- label la `log1p(popularity)`
- split criterion la squared error

Vi vay, ten "LightGBM" trong code hien tai dung hon cho:

- artifact/scoring format
- feature contract va runtime scorer

Chu chua dung nghia "train bang LightGBM framework that".

## 8. Chat luong feature va label hien tai

Offline training hien tai van mang tinh proxy:

- `upvote_ratio` default cung `0.5`
- interaction features phan lon la placeholder
- `hours_since_last_interaction` default `999.0`
- label dung `log1p(popularity)` thay vi user engagement label that

Online extraction cung chua day du behavior signal:

- `engagementRate` dang hardcode `0.0`
- `interactionCount7d` va `interactionCount30d` dang `0`
- affinity chu yeu dua tren follow relation

He qua:

- pipeline da chay duoc end-to-end
- nhung model hien tai nghieng ve "content popularity proxy" hon la "personalized feed ranking"

## 9. Diem manh cua thiet ke hien tai

- Co feature contract tap trung mot cho.
- Co local scorer thuần Java, khong can native LightGBM runtime.
- Artifact format co metadata schema version.
- Runtime co guard:
  - model missing -> bo qua
  - schema mismatch -> bo qua
  - prediction set invalid -> fallback
- Training code da du clean de co the tach ra module rieng sau nay.

## 10. Diem coupling va rui ro chinh

### 10.1. Coupling hai chieu giua `ai` va `feed`

Day la diem lon nhat.

- `feed` can `ai` de score
- `ai` can DTO/use case cua `feed` de vectorize va tra ket qua

Ket qua:

- kho tach `ai` thanh Maven module rieng
- de vong phu thuoc neu tiep tuc day manh module hoa

### 10.2. Training va runtime song chung trong backend source set

Dieu nay co nghia:

- build backend cung keo theo training code
- dependency cho offline pipeline nam trong `backend/pom.xml`
- boundary deploy/runtime va offline pipeline chua ro

### 10.3. Artifact format va trainer implementation co the gay nham

Ten package la `lightgbm`, nhung trainer la custom tree booster.
Neu team nghi rang hien tai dang train bang LightGBM that, se de danh gia sai:

- hyperparameter semantics
- model parity voi Python LightGBM
- expected scoring behavior

## 11. Neu muon tach module sau nay, nen tach o dau

Huong tach thuc te nhat:

1. Tach `training` ra truoc.
   - vi no da gan doc lap
   - no chi can shared feature contract
2. Tach `lightgbm` core thanh shared artifact package.
   - `LightGbmFeatureSchema`
   - `LightGbmModel`
   - `LightGbmModelArtifact`
   - `LightGbmModelScorer`
3. Giu `LightGbmFeatureVectorizer` va `LightGbmRankingService` o backend feed layer, hoac doi chung sang contract trung gian.
4. Neu muon `ai` doc lap hon nua, thay dependency vao `RankingFeatures` bang mot DTO trung gian khong thuoc `feed`.

Noi ngan gon:

- `training` la phan de tach nhat
- `vectorizer` va `ranking service` la phan dang dinh chat voi backend feed

## 12. Tom tat mot cau

`com.socialpulse.app.ai` hien tai la mot package AI noi bo cua backend, gom offline Pushshift training + JSON model scoring runtime; no da co feature contract va fallback kha ro, nhung van coupling chat hai chieu voi `feed`, nen chua phai mot module `ai-training -> export artifact -> backend consume` tach biet hoan toan.
