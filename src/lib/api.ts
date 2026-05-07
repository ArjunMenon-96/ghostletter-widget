// UPDATE THIS after Vercel deployment — replace with your actual Vercel URL
export const API_BASE_URL = "https://ghostletter111.vercel.app";

export type NewsItem = {
  id: string;
  title: string;
  link: string;
  pubDate: string;
  summary: string;
  source: string;
  sourceColor: string;
  category: string;
  imageUrl?: string;
};

export type NewsResponse = {
  items: NewsItem[];
  total: number;
};

export async function fetchNews(
  category = "All",
  search = ""
): Promise<NewsItem[]> {
  const params = new URLSearchParams();
  if (category !== "All") params.set("category", category);
  if (search) params.set("search", search);

  const res = await fetch(`${API_BASE_URL}/api/news?${params.toString()}`);
  if (!res.ok) throw new Error("Failed to fetch news");
  const data: NewsResponse = await res.json();
  return data.items;
}

export const CATEGORIES = [
  "All",
  "News",
  "Markets",
  "DeFi",
  "Bitcoin",
  "Research",
];
