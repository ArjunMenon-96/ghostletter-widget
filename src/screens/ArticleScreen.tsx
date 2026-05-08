import React, { useState } from "react";
import {
  ActivityIndicator,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { WebView } from "react-native-webview";
import { NewsItem } from "../lib/api";

type Props = {
  item: NewsItem;
  onBack: () => void;
};

export default function ArticleScreen({ item, onBack }: Props) {
  const [loading, setLoading] = useState(true);

  return (
    <SafeAreaView style={styles.container} edges={["top"]}>
      {/* Toolbar */}
      <View style={styles.toolbar}>
        <TouchableOpacity style={styles.backBtn} onPress={onBack}>
          <Text style={styles.backArrow}>←</Text>
          <Text style={styles.backText}>Back</Text>
        </TouchableOpacity>
        <View style={styles.toolbarCenter}>
          <View
            style={[styles.sourceDot, { backgroundColor: item.sourceColor }]}
          />
          <Text style={styles.toolbarSource} numberOfLines={1}>
            {item.source}
          </Text>
        </View>
      </View>

      {loading && (
        <View style={styles.loadingOverlay} pointerEvents="none">
          <ActivityIndicator size="large" color="#f5a623" />
        </View>
      )}

      <WebView
        source={{ uri: item.link }}
        style={styles.webview}
        onLoadEnd={() => setLoading(false)}
        onError={() => setLoading(false)}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#0a0a08" },
  toolbar: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "#111109",
    borderBottomWidth: 1,
    borderBottomColor: "#2a2a20",
    paddingHorizontal: 14,
    paddingVertical: 10,
    gap: 12,
  },
  backBtn: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
    borderWidth: 1,
    borderColor: "#2a2a20",
    paddingHorizontal: 10,
    paddingVertical: 5,
  },
  backArrow: {
    color: "#f5a623",
    fontSize: 14,
    fontWeight: "700",
  },
  backText: {
    color: "#f5a623",
    fontSize: 10,
    fontWeight: "700",
    letterSpacing: 1,
  },
  toolbarCenter: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
  },
  sourceDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
  },
  toolbarSource: {
    color: "#a89060",
    fontSize: 10,
    fontWeight: "700",
    letterSpacing: 0.5,
    flex: 1,
    textTransform: "uppercase",
  },
  webview: {
    flex: 1,
    backgroundColor: "#0a0a08",
  },
  loadingOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "#0a0a08",
    alignItems: "center",
    justifyContent: "center",
    zIndex: 10,
  },
});
