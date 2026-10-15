import { useEffect, useState } from 'react';

export interface AsyncState<T> { data: T | undefined; loading: boolean; error: string | undefined }

/** Runs `fn` whenever `deps` change; pass null to stay idle. Ignores stale responses. */
export function useAsync<T>(fn: (() => Promise<T>) | null, deps: unknown[]): AsyncState<T> {
  const [state, setState] = useState<AsyncState<T>>({ data: undefined, loading: fn !== null, error: undefined });
  useEffect(() => {
    if (!fn) { setState({ data: undefined, loading: false, error: undefined }); return; }
    let cancelled = false;
    setState((s) => ({ data: s.data, loading: true, error: undefined }));
    fn().then(
      (data) => { if (!cancelled) setState({ data, loading: false, error: undefined }); },
      (e: unknown) => { if (!cancelled) setState({ data: undefined, loading: false, error: e instanceof Error ? e.message : 'Request failed.' }); },
    );
    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
  return state;
}
