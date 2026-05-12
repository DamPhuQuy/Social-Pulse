import pandas as pd
import os


class DataCleaning:
    def __init__(self, file_path: str, debug: bool = False):
        self.__file_path = file_path
        self.debug = debug
        self.output_dir = "cleaned"
        os.makedirs(self.output_dir, exist_ok=True)

    def load_data(self) -> pd.DataFrame:
        return pd.read_csv(self.__file_path)

    def drop_unnecessary_columns(self, df: pd.DataFrame) -> pd.DataFrame:
        unnecessary_columns = [
            "type",
            "id",
            "subreddit.id",
            "subreddit.name",
            "subreddit.nsfw",
            "permalink",
            "domain",
            "url",
        ]
        df = df.drop(columns=unnecessary_columns, errors="ignore")

        if self.debug:
            df.to_csv(f"{self.output_dir}/step1_drop_columns.csv", index=False)

        return df

    def drop_null_rows(self, df: pd.DataFrame) -> pd.DataFrame:
        df = df.dropna(subset=["selftext"])

        if self.debug:
            df.to_csv(f"{self.output_dir}/step2_drop_null.csv", index=False)

        return df

    def drop_duplicates(self, df: pd.DataFrame) -> pd.DataFrame:
        df = df.drop_duplicates()

        if self.debug:
            df.to_csv(f"{self.output_dir}/step3_drop_duplicates.csv", index=False)

        return df

    def drop_deleted_rows(self, df: pd.DataFrame) -> pd.DataFrame:
        df["selftext"] = df["selftext"].str.strip().str.lower()

        df = df[(df["selftext"] != "[deleted]") & (df["selftext"] != "[removed]")]

        if self.debug:
            df.to_csv(f"{self.output_dir}/step4_drop_deleted.csv", index=False)

        return df

    def clean_data(self) -> pd.DataFrame:
        df = self.load_data()
        df = self.drop_unnecessary_columns(df)
        df = self.drop_null_rows(df)
        df = self.drop_duplicates(df)
        df = self.drop_deleted_rows(df)

        # save final
        df.to_csv(f"{self.output_dir}/final_cleaned.csv", index=False)

        return df

if __name__ == "__main__":
    cleaner = DataCleaning(file_path="data/reddit_posts.csv", debug=True)
    cleaner.clean_data()
