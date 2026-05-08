import React, { useState } from "react";
import { SafeAreaProvider } from "react-native-safe-area-context";
import HomeScreen from "./src/screens/HomeScreen";
import { NewsItem } from "./src/lib/api";
import { Linking } from "react-native";

export default function App() {
  const [_, setSelected] = useState<NewsItem | null>(null);

  function openArticle(item: NewsItem) {
    setSelected(item);
    Linking.openURL(item.link);
  }

  return (
    <SafeAreaProvider>
      <HomeScreen onArticlePress={openArticle} />
    </SafeAreaProvider>
  );
}
