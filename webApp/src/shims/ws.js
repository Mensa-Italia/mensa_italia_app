// Browser shim for the Node-only `ws` module.
// Ktor's JS engine declares `ws` as a runtime dependency but the import path is
// gated by a Node check (`typeof window === 'undefined'`). In the browser the
// native `WebSocket` is always used. Stubbing satisfies Vite's import scanner.
const WS = globalThis.WebSocket;
export default WS;
export { WS as WebSocket };
