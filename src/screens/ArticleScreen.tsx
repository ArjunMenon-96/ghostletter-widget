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
          <ActivityIndicator size="large" color="#7c3aed" />
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
  container: { flex: 1, backgroundColor: "#09090b" },
  toolbar: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "#09090b",
    borderBottomWidth: 1,
    borderBottomColor: "#27272a",
    paddingHorizontal: 12,
    paddingVertical: 10,
    gap: 12,
  },
  backBtn: {
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
  },
  backArrow: {
    color: "#a78bfa",
    fontSize: 18,
  },
  backText: {
    color: "#a78bfa",
    fontSize: 14,
    fontWeight: "500",
  },
  toolbarCenter: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
  },
  sourceDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  toolbarSource: {
    color: "#a1a1aa",
    fontSize: 13,
    flex: 1,
  },
  webview: {
    flex: 1,
    backgroundColor: "#09090b",
  },
  loadingOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "#09090b",
    alignItems: "center",
    justifyContent: "center",
    zIndex: 10,
  },
});
