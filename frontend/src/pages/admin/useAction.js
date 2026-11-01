import { useState } from 'react';

/** Wraps async admin actions with a shared error state. */
export function useAction() {
  const [error, setError] = useState(null);
  const run = async (action) => {
    setError(null);
    try {
      await action();
      return true;
    } catch (e) {
      setError(e);
      return false;
    }
  };
  return { error, run };
}
