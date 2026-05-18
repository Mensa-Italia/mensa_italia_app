// Classic Web Worker that hosts a sql.js in-memory SQLite database, driving
// the SQLDelight WebWorkerDriver protocol. Adapted from
// @cashapp/sqldelight-sqljs-worker/sqljs.worker.js (which uses ES module
// `import` syntax that requires Worker({type:'module'})) into a classic
// script that uses `importScripts` so Kotlin/JS's plain `new Worker(url)`
// can load it.
//
// Companion files (must be served alongside this one in the public dir):
//   /sql-wasm.js   — the sql.js loader (defines `initSqlJs` as a global)
//   /sql-wasm.wasm — the actual SQLite WebAssembly module

importScripts("/sql-wasm.js");

let db = null;

async function createDatabase() {
  const SQL = await initSqlJs({ locateFile: () => "/sql-wasm.wasm" });
  db = new SQL.Database();
}

function onModuleReady() {
  const data = this.data;
  switch (data && data.action) {
    case "exec":
      if (!data["sql"]) {
        throw new Error("exec: Missing query string");
      }
      return postMessage({
        id: data.id,
        results: db.exec(data.sql, data.params)[0] ?? { values: [] },
      });
    case "begin_transaction":
      return postMessage({ id: data.id, results: db.exec("BEGIN TRANSACTION;") });
    case "end_transaction":
      return postMessage({ id: data.id, results: db.exec("END TRANSACTION;") });
    case "rollback_transaction":
      return postMessage({ id: data.id, results: db.exec("ROLLBACK TRANSACTION;") });
    default:
      throw new Error(`Unsupported action: ${data && data.action}`);
  }
}

function onError(err) {
  return postMessage({ id: this.data.id, error: err });
}

const sqlModuleReady = createDatabase();
self.onmessage = (event) => {
  return sqlModuleReady.then(onModuleReady.bind(event)).catch(onError.bind(event));
};
