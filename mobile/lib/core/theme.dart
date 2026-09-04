import 'package:flutter/material.dart';

final appTheme = ThemeData(
  useMaterial3: true,
  colorSchemeSeed: const Color(0xFF0F766E),
  scaffoldBackgroundColor: const Color(0xFFF1F5F9),
  appBarTheme: const AppBarTheme(centerTitle: false, elevation: 0),
  cardTheme: CardTheme(
    elevation: 0,
    shape: RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(14),
      side: const BorderSide(color: Color(0xFFE2E8F0)),
    ),
  ),
  inputDecorationTheme: const InputDecorationTheme(border: OutlineInputBorder()),
);

Color statusColor(String? s) {
  switch (s) {
    case 'PURCHASED':
    case 'BOOKING':
    case 'INTERESTED':
      return const Color(0xFF16A34A);
    case 'LOST':
    case 'NOT_INTERESTED':
      return const Color(0xFFDC2626);
    case 'SITE_VISIT_SCHEDULED':
    case 'SITE_VISIT_DONE':
    case 'NEGOTIATION':
      return const Color(0xFF7C3AED);
    case 'NEW':
      return const Color(0xFF64748B);
    default:
      return const Color(0xFF2563EB);
  }
}
