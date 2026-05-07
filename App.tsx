import React, { useState } from "react";
import { registerWidgetTaskHandler } from "react-native-android-widget";
import { SafeAreaProvider } from "react-native-safe-area-context";
import ArticleScreen from "./src/screens/ArticleScreen";
import HomeScreen from "./src/screens/HomeScreen";
import { widgetTaskHandler } from "./src/widget/widgetTask";
import { NewsItem } from "./src/lib/api";

// Register widget task handler (runs in headless mode for home screen widget)
registerWidgetTaskHandler(widgetTaskHandler);

type Screen = "home" | "article";

export default function App() {
  const [screen, setScreen] = useState<Screen>("home");
  const [selectedArticle, setSelectedArticle] = useState<NewsItem | null>(null);

  function openArticle(item: NewsItem) {
    setSelectedArticle(item);
    setScreen("article");
  }

  function goHome() {
    setScreen("home");
    setSelectedArticle(null);
  }

  return (
    <SafeAreaProvider>
      {screen === "home" && <HomeScreen onArticlePress={openArticle} />}
      {screen === "article" && selectedArticle && (
        <ArticleScreen item={selectedArticle} onBack={goHome} />
      )}
    </SafeAreaProvider>
  );
}
