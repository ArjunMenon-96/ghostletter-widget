import React, { useEffect, useState } from "react";
import {
  FlatList,
  RefreshControl,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import CategoryPills from "../components/CategoryPills";
import NewsCard from "../components/NewsCard";
import { NewsItem } from "../lib/api";
import { useNews } from "../hooks/useNews";

const TICKERS = [
  { sym: "BTC", price: "$97,240", chg: "+2.41%", up: true },
  { sym: "ETH", price: "$3,182", chg: "+1.83%", up: true },
  { sym: "SOL", price: "$178.40", chg: "+5.12%", up: true },
  { sym: "BNB", price: "$612", chg: "-0.34%", up: false },
  { sym: "ARB", price: "$1.24", chg: "+12.1%", up: true },
  { sym: "AVAX", price: "$38.20", chg: "+3.71%", up: true },
];

type Props = {
  onArticlePress: (item: NewsItem) => void;
};

export default function HomeScreen({ onArticlePress }: Props) {
  const [category, setCategory] = useState("All");
  const [search, setSearch] = useState("");
  const [time, setTime] = useState("");
  const { items, loading, error, lastUpdated, refresh } = useNews(category, search);

  useEffect(() => {
    function tick() {
      setTime(
        new Date().toLocaleTimeString("en-GB", {
          timeZone: "UTC",
          hour: "2-digit",
          minute: "2-digit",
          second: "2-digit",
        }) + " UTC"
      );
    }
    tick();
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
  }, []);

  return (
    <SafeAreaView style={styles.container} edges={["top"]}>
      <StatusBar barStyle="light-content" backgroundColor="#111109" />

      {/* Top bar */}
      <View style={styles.topBar}>
        <Text style={styles.logo}>GHOSTLETTER</Text>
        <View style={styles.livePill}>
          <View style={styles.liveDot} />
          <Text style={styles.liveText}>LIVE</Text>
        </View>
        <Text style={styles.clock}>{time}</Text>
      </View>

      {/* Ticker strip */}
      <View style={styles.tickerWrap}>
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.tickerInner}
        >
          {TICKERS.map((t) => (
            <View key={t.sym} style={styles.tickerItem}>
              <Text style={styles.tickerSym}>{t.sym}</Text>
              <Text style={styles.tickerPrice}>{t.price}</Text>
              <Text style={[styles.tickerChg, t.up ? styles.green : styles.red]}>
                {t.up ? "▲" : "▼"} {t.chg}
              </Text>
            </View>
          ))}
        </ScrollView>
      </View>

      {/* Stats bar */}
      <View style={styles.statsBar}>
        {[
          { label: "STORIES", value: loading ? "—" : String(items.length) },
          { label: "SOURCES", value: "10" },
          { label: "REFRESH", value: "5 MIN" },
          {
            label: "UPDATED",
            value: lastUpdated
              ? lastUpdated.toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit" })
              : "—",
          },
        ].map((s) => (
          <View key={s.label} style={styles.statCell}>
            <Text style={styles.statLabel}>{s.label}</Text>
            <Text style={styles.statValue}>{s.value}</Text>
          </View>
        ))}
      </View>

      {/* Category tabs */}
      <CategoryPills active={category} onChange={setCategory} />

      {/* Search + panel header */}
      <View style={styles.panelHeader}>
        <Text style={styles.panelTitle}>LIVE FEED</Text>
        <View style={styles.updatingBadge}>
          <Text style={styles.updatingText}>● UPDATING</Text>
        </View>
        <TextInput
          style={styles.searchInput}
          placeholder="SEARCH..."
          placeholderTextColor="#3a3a30"
          value={search}
          onChangeText={setSearch}
          returnKeyType="search"
        />
        <TouchableOpacity onPress={refresh} style={styles.refreshBtn}>
          <Text style={styles.refreshIcon}>{loading ? "↻" : "↺"}</Text>
        </TouchableOpacity>
      </View>

      {/* Feed */}
      {loading && items.length === 0 ? (
        <View style={styles.center}>
          {[...Array(5)].map((_, i) => (
            <View key={i} style={styles.skeleton}>
              <View style={styles.skelThumb} />
              <View style={styles.skelText}>
                <View style={styles.skelMeta} />
                <View style={styles.skelTitle} />
                <View style={styles.skelSub} />
              </View>
            </View>
          ))}
        </View>
      ) : error ? (
        <View style={styles.centerMsg}>
          <Text style={styles.errorText}>⚠ {error}</Text>
        </View>
      ) : (
        <FlatList
          data={items}
          keyExtractor={(item) => item.id}
          renderItem={({ item, index }) => (
            <NewsCard
              item={item}
              index={index}
              onPress={() => onArticlePress(item)}
            />
          )}
          showsVerticalScrollIndicator={false}
          refreshControl={
            <RefreshControl
              refreshing={loading}
              onRefresh={refresh}
              tintColor="#f5a623"
              colors={["#f5a623"]}
              progressBackgroundColor="#111109"
            />
          }
          ListEmptyComponent={
            <View style={styles.centerMsg}>
              <Text style={styles.emptyText}>NO RESULTS — TRY A DIFFERENT QUERY</Text>
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
    backgroundColor: "#0a0a08",
  },

  /* Top bar */
  topBar: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 14,
    paddingVertical: 8,
    backgroundColor: "#111109",
    borderBottomWidth: 1,
    borderBottomColor: "#1a1a14",
    gap: 10,
  },
  logo: {
    color: "#f5a623",
    fontSize: 13,
    fontWeight: "700",
    letterSpacing: 3,
    flex: 1,
  },
  livePill: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "#f5a62318",
    borderWidth: 1,
    borderColor: "#f5a62340",
    paddingHorizontal: 7,
    paddingVertical: 3,
    gap: 5,
    borderRadius: 2,
  },
  liveDot: {
    width: 5,
    height: 5,
    borderRadius: 3,
    backgroundColor: "#f5a623",
  },
  liveText: {
    color: "#f5a623",
    fontSize: 8,
    fontWeight: "700",
    letterSpacing: 1,
  },
  clock: {
    color: "#666",
    fontSize: 9,
  },

  /* Ticker */
  tickerWrap: {
    backgroundColor: "#111109",
    borderBottomWidth: 1,
    borderBottomColor: "#2a2a20",
  },
  tickerInner: {
    paddingHorizontal: 14,
    paddingVertical: 6,
    gap: 24,
    flexDirection: "row",
  },
  tickerItem: {
    flexDirection: "row",
    alignItems: "center",
    gap: 5,
  },
  tickerSym: {
    color: "#f5a623",
    fontSize: 10,
    fontWeight: "700",
  },
  tickerPrice: {
    color: "#a89060",
    fontSize: 10,
  },
  tickerChg: {
    fontSize: 10,
    fontWeight: "600",
  },
  green: { color: "#52c41a" },
  red: { color: "#ff4d4f" },

  /* Stats */
  statsBar: {
    flexDirection: "row",
    backgroundColor: "#0d0d0a",
    borderBottomWidth: 1,
    borderBottomColor: "#1a1a14",
  },
  statCell: {
    flex: 1,
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRightWidth: 1,
    borderRightColor: "#1a1a14",
  },
  statLabel: {
    fontSize: 7,
    color: "#3a3a30",
    letterSpacing: 0.8,
    marginBottom: 2,
    fontWeight: "700",
  },
  statValue: {
    fontSize: 15,
    fontWeight: "700",
    color: "#f5a623",
  },

  /* Panel header */
  panelHeader: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 14,
    paddingVertical: 7,
    backgroundColor: "#111109",
    borderBottomWidth: 1,
    borderBottomColor: "#2a2a20",
    gap: 8,
  },
  panelTitle: {
    color: "#f5a623",
    fontSize: 10,
    fontWeight: "700",
    letterSpacing: 1,
  },
  updatingBadge: {
    backgroundColor: "#f5a62318",
    borderWidth: 1,
    borderColor: "#f5a62340",
    borderRadius: 2,
    paddingHorizontal: 5,
    paddingVertical: 2,
  },
  updatingText: {
    color: "#f5a623",
    fontSize: 7,
    fontWeight: "700",
    letterSpacing: 0.8,
  },
  searchInput: {
    flex: 1,
    backgroundColor: "#0a0a08",
    borderWidth: 1,
    borderColor: "#2a2a20",
    color: "#d4c08a",
    paddingHorizontal: 10,
    paddingVertical: 5,
    fontSize: 10,
    letterSpacing: 0.5,
    borderRadius: 0,
  },
  refreshBtn: {
    borderWidth: 1,
    borderColor: "#2a2a20",
    paddingHorizontal: 10,
    paddingVertical: 5,
  },
  refreshIcon: {
    color: "#555",
    fontSize: 14,
  },

  /* Skeletons */
  center: {
    flex: 1,
  },
  skeleton: {
    flexDirection: "row",
    borderBottomWidth: 1,
    borderBottomColor: "#1a1a14",
    height: 80,
  },
  skelThumb: {
    width: 84,
    height: 80,
    backgroundColor: "#111109",
  },
  skelText: {
    flex: 1,
    padding: 12,
    justifyContent: "center",
    gap: 6,
  },
  skelMeta: {
    height: 8,
    width: 100,
    backgroundColor: "#1a1a14",
    borderRadius: 1,
  },
  skelTitle: {
    height: 11,
    width: "90%",
    backgroundColor: "#161610",
    borderRadius: 1,
  },
  skelSub: {
    height: 9,
    width: "65%",
    backgroundColor: "#131310",
    borderRadius: 1,
  },

  /* States */
  centerMsg: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    padding: 40,
  },
  errorText: {
    color: "#ff4d4f",
    fontSize: 11,
    fontWeight: "700",
    letterSpacing: 0.5,
  },
  emptyText: {
    color: "#333",
    fontSize: 10,
    fontWeight: "700",
    letterSpacing: 1,
    textAlign: "center",
  },
});
