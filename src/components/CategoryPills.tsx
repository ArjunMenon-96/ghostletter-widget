import React from "react";
import { ScrollView, StyleSheet, Text, TouchableOpacity } from "react-native";
import { CATEGORIES } from "../lib/api";

type Props = {
  active: string;
  onChange: (cat: string) => void;
};

export default function CategoryPills({ active, onChange }: Props) {
  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      style={styles.scroll}
      contentContainerStyle={styles.container}
    >
      {CATEGORIES.map((cat) => (
        <TouchableOpacity
          key={cat}
          onPress={() => onChange(cat)}
          style={[styles.pill, active === cat && styles.pillActive]}
          activeOpacity={0.7}
        >
          <Text style={[styles.text, active === cat && styles.textActive]}>
            {cat}
          </Text>
        </TouchableOpacity>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  scroll: { flexGrow: 0 },
  container: { paddingHorizontal: 16, paddingVertical: 10, gap: 8 },
  pill: {
    backgroundColor: "#27272a",
    borderRadius: 20,
    paddingHorizontal: 14,
    paddingVertical: 6,
  },
  pillActive: {
    backgroundColor: "#7c3aed",
  },
  text: {
    color: "#a1a1aa",
    fontSize: 13,
    fontWeight: "500",
  },
  textActive: {
    color: "#fff",
  },
});
