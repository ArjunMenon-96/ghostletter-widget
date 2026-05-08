import React from "react";
import { ScrollView, StyleSheet, Text, TouchableOpacity, View } from "react-native";
import { CATEGORIES } from "../lib/api";

type Props = {
  active: string;
  onChange: (cat: string) => void;
};

export default function CategoryPills({ active, onChange }: Props) {
  return (
    <View style={styles.wrapper}>
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.container}
      >
        {CATEGORIES.map((cat) => {
          const isActive = active === cat;
          return (
            <TouchableOpacity
              key={cat}
              onPress={() => onChange(cat)}
              style={[styles.tab, isActive && styles.tabActive]}
              activeOpacity={0.7}
            >
              <Text style={[styles.text, isActive && styles.textActive]}>
                {cat.toUpperCase()}
              </Text>
              {isActive && <View style={styles.underline} />}
            </TouchableOpacity>
          );
        })}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: {
    borderBottomWidth: 1,
    borderBottomColor: "#2a2a20",
    backgroundColor: "#0d0d0a",
  },
  container: {
    paddingHorizontal: 4,
    flexDirection: "row",
  },
  tab: {
    paddingHorizontal: 14,
    paddingVertical: 9,
    position: "relative",
    borderRightWidth: 1,
    borderRightColor: "#1a1a14",
  },
  tabActive: {
    backgroundColor: "#111109",
  },
  text: {
    color: "#444",
    fontSize: 9,
    fontWeight: "700",
    letterSpacing: 0.8,
  },
  textActive: {
    color: "#f5a623",
  },
  underline: {
    position: "absolute",
    bottom: 0,
    left: 0,
    right: 0,
    height: 2,
    backgroundColor: "#f5a623",
  },
});
