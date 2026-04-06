import { HarvestRecord } from '@/src/types';

const STORAGE_KEY = 'palm_harvest_records';

export const saveRecordLocal = (record: HarvestRecord) => {
  const records = getRecordsLocal();
  const index = records.findIndex(r => r.id === record.id);
  if (index !== -1) {
    records[index] = record;
  } else {
    records.unshift(record);
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(records));
};

export const getRecordsLocal = (): HarvestRecord[] => {
  const data = localStorage.getItem(STORAGE_KEY);
  return data ? JSON.parse(data) : [];
};

export const saveRecordsLocal = (records: HarvestRecord[]) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(records));
};

export const clearRecordsLocal = () => {
  localStorage.removeItem(STORAGE_KEY);
};
