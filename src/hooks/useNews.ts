import { useCallback, useEffect, useRef, useState } from "react";
import { fetchNews, NewsItem } from "../lib/api";

export function useNews(category: string, search: string) {
  const [items, setItems] = useState<NewsItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchNews(category, search);
      setItems(data);
      setLastUpdated(new Date());
    } catch {
      setError("Could not load news. Check your connection.");
    } finally {
      setLoading(false);
    }
  }, [category, search]);

  useEffect(() => {
    load();
    intervalRef.current = setInterval(load, 5 * 60 * 1000);
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [load]);

  return { items, loading, error, lastUpdated, refresh: load };
}
