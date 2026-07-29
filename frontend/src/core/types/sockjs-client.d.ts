declare module "sockjs-client" {
  export interface SockJSOptions {
    transports?: string[];
    timeout?: number;
    sessionId?: () => string;
  }

  export default class SockJS {
    constructor(url: string, protocols?: string | string[], options?: SockJSOptions);
  }
}
