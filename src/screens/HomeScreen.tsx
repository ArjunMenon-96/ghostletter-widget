import React, { useState } from "react";
import {
  ActivityIndicator,
  FlatList,
  RefreshControl,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import CategoryPills from "../components/CategoryPills";
import NewsCard from "../components/NewsCard";
import { NewsItem } from "../lib/api";
import { useNews } from "../hooks/useNews";

type Props = {
  onArticlePress: (item: NewsItem) => void;
};

export default function HomeScreen({ onArticlePress }: Props) {
  const [category, setCategory] = useState("All");
  const [search, setSearch] = useState("");
  const { items, loading, error, lastUpdated, refresh } = useNews(
    category,
    search
  );

  return (
    <SafeAreaView style={styles.container} edges={["top"]}>
      <StatusBar barStyle="light-content" backgroundColor="#09090b" />

      {/* Header */}
      <View style={styles.header}>
        <View style={styles.logoRow}>
          <View style={styles.logoBadge}>
            <Text style={styles.logoText}>₿</Text>
          </View>
          <View>
            <Text style={styles.appName}>GhostLetter</Text>
            <Text style={styles.tagline}>DeFi · Blockchain · Fintech</Text>
          </View>
          <View style={styles.liveBadge}>
            <View style={styles.liveDot} />
            <Text style={styles.liveText}>LIVE</Text>
          </View>
        </View>

        <View style={styles.searchRow}>
          <TextInput
            style={styles.searchInput}
            placeholder="Search news, tokens, sources..."
            placeholderTextColor="#52525b"
            value={search}
            onChangeText={setSearch}
            returnKeyType="search"
          />
        </View>
      </View>

      <CategoryPills active={category} onChange={setCategory} />

      {lastUpdated && (
        <Text style={styles.updatedText}>
          Updated {lastUpdated.toLocaleTimeString()} · Auto-refreshes every 5 min
        </Text>
      )}

      {loading && items.length === 0 ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color="#7c3aed" />
          <Text style={styles.loadingText}>Fetching latest news...</Text>
        </View>
      ) : error ? (
        <View style={styles.center}>
          <Text style={styles.errorText}>⚠️ {error}</Text>
        </View>
      ) : (
        <FlatList
          data={items}
          keyExtractor={(item) => item.id}
          renderItem={({ item }) => (
            <NewsCard item={item} onPress={() => onArticlePress(item)} />
          )}
          contentContainerStyle={styles.list}
          showsVerticalScrollIndicator={false}
          refreshControl={
            <RefreshControl
              refreshing={loading}
              onRefresh={refresh}
              tintColor="#7c3aed"
              colors={["#7c3aed"]}
              progressBackgroundColor="#18181b"
            />
          }
          ListEmptyComponent={
            <View style={styles.center}>
              <Text style={styles.emptyText}>No articles found</Text>
            </View>
          }
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#09090b",
  },
  header: {
    paddingHorizontal: 16,
    paddingTop: 8,
    paddingBottom: 4,
  },
  logoRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 10,
    marginBottom: 12,
  },
  logoBadge: {
    width: 36,
    height: 36,
    borderRadius: 10,
    backgroundColor: "#7c3aed",
    alignItems: "center",
    justifyContent: "center",
  },
  logoText: {
    color: "#fff",
    fontSize: 18,
    fontWeight: "700",
  },
  appName: {
    color: "#f4f4f5",
    fontSize: 18,
    fontWeight: "700",
    letterSpacing: -0.3,
  },
  tagline: {
    color: "#71717a",
    fontSize: 11,
  },
  liveBadge: {
    marginLeft: "auto",
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "#7c3aed22",
    borderColor: "#7c3aed44",
    borderWidth: 1,
    borderRadius: 20,
    paddingHorizontal: 8,
    paddingVertical: 4,
    gap: 5,
  },
  liveDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: "#a78bfa",
  },
  liveText: {
    color: "#a78bfa",
    fontSize: 10,
    fontWeight: "700",
    letterSpacing: 1,
  },
  searchRow: {
    flexDirection: "row",
    gap: 8,
  },
  searchInput: {
    flex: 1,
    backgroundColor: "#18181b",
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "#27272a",
    color: "#f4f4f5",
    paddingHorizontal: 14,
    paddingVertical: 10,
    fontSize: 14,
  },
  updatedText: {
    color: "#3f3f46",
    fontSize: 10,
    paddingHorizontal: 16,
    marginBottom: 4,
  },
  list: {
    padding: 16,
    paddingTop: 8,
  },
  center: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    padding: 40,
  },
  loadingText: {
    color: "#71717a",
    marginTop: 12,
    fontSize: 14,
  },
  errorText: {
    color: "#f87171",
    fontSize: 14,
    textAlign: "center",
  },
  emptyText: {
    color: "#52525b",
    fontSize: 14,
  },
});
