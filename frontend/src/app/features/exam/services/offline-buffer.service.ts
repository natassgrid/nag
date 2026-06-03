import { Injectable } from '@angular/core';
import { ResponseSave } from './exam.service';

interface BufferedResponse extends ResponseSave {
  timestamp: number;
  sessionId: string;
  synced: boolean;
}

@Injectable({ providedIn: 'root' })
export class OfflineBufferService {
  private readonly DB_NAME = 'exam_offline_db';
  private readonly STORE_NAME = 'responses';
  private readonly DB_VERSION = 1;
  private db: IDBDatabase | null = null;

  async init(): Promise<void> {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(this.DB_NAME, this.DB_VERSION);

      request.onupgradeneeded = (event) => {
        const db = (event.target as IDBOpenDBRequest).result;
        if (!db.objectStoreNames.contains(this.STORE_NAME)) {
          const store = db.createObjectStore(this.STORE_NAME, { keyPath: 'id', autoIncrement: true });
          store.createIndex('sessionId', 'sessionId', { unique: false });
          store.createIndex('synced', 'synced', { unique: false });
        }
      };

      request.onsuccess = (event) => {
        this.db = (event.target as IDBOpenDBRequest).result;
        resolve();
      };

      request.onerror = () => reject(request.error);
    });
  }

  async bufferResponse(sessionId: string, response: ResponseSave): Promise<void> {
    if (!this.db) await this.init();

    const buffered: BufferedResponse = {
      ...response,
      sessionId,
      timestamp: Date.now(),
      synced: false
    };

    return new Promise((resolve, reject) => {
      const tx = this.db!.transaction(this.STORE_NAME, 'readwrite');
      const store = tx.objectStore(this.STORE_NAME);
      const request = store.add(buffered);
      request.onsuccess = () => resolve();
      request.onerror = () => reject(request.error);
    });
  }

  async getUnsyncedResponses(sessionId: string): Promise<BufferedResponse[]> {
    if (!this.db) await this.init();

    return new Promise((resolve, reject) => {
      const tx = this.db!.transaction(this.STORE_NAME, 'readonly');
      const store = tx.objectStore(this.STORE_NAME);
      const index = store.index('sessionId');
      const request = index.getAll(sessionId);

      request.onsuccess = () => {
        const results = (request.result as BufferedResponse[]).filter(r => !r.synced);
        resolve(results.sort((a, b) => a.timestamp - b.timestamp));
      };
      request.onerror = () => reject(request.error);
    });
  }

  async markSynced(ids: number[]): Promise<void> {
    if (!this.db) return;

    const tx = this.db.transaction(this.STORE_NAME, 'readwrite');
    const store = tx.objectStore(this.STORE_NAME);

    for (const id of ids) {
      const getReq = store.get(id);
      getReq.onsuccess = () => {
        const record = getReq.result;
        if (record) {
          record.synced = true;
          store.put(record);
        }
      };
    }
  }

  async clearSession(sessionId: string): Promise<void> {
    if (!this.db) return;

    const tx = this.db.transaction(this.STORE_NAME, 'readwrite');
    const store = tx.objectStore(this.STORE_NAME);
    const index = store.index('sessionId');
    const request = index.getAllKeys(sessionId);

    request.onsuccess = () => {
      for (const key of request.result) {
        store.delete(key);
      }
    };
  }
}
