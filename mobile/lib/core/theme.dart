import 'package:flutter/material.dart';

const _brandNavy = Color(0xFF16294D);
const _brandGold = Color(0xFFC9A227);

final appTheme = ThemeData(
  useMaterial3: true,
  colorScheme: ColorScheme.fromSeed(
    seedColor: _brandNavy,
    primary: _brandNavy,
    secondary: _brandGold,
  ),
  scaffoldBackgroundColor: const Color(0xFFF1F5F9),
  appBarTheme: const AppBarTheme(
    centerTitle: false,
    elevation: 0,
    foregroundColor: _brandNavy,
    surfaceTintColor: Colors.transparent,
  ),
  cardTheme: CardTheme(
    elevation: 0,
    shape: RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(14),
      side: const BorderSide(color: Color(0xFFE2E8F0)),
    ),
  ),
  inputDecorationTheme: const InputDecorationTheme(
    border: OutlineInputBorder(),
    focusedBorder: OutlineInputBorder(borderSide: BorderSide(color: _brandGold, width: 2)),
  ),
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
