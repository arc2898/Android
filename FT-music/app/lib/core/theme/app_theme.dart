import 'package:flutter/material.dart';

/// FT-music design tokens. All screens should use these values instead of
/// introducing one-off colors, radii, or motion curves.
class AppTheme {
  static const primaryTextStyle = TextStyle(fontFamily: 'Unageo');
  static const secondoryTextStyle = TextStyle(fontFamily: 'Unageo');
  static const secondoryTextStyleMedium =
      TextStyle(fontFamily: 'Unageo', fontWeight: FontWeight.w700);
  static const tertiaryTextStyle = TextStyle(fontFamily: 'CodePro');
  static const fontAwesomeRegularFont =
      TextStyle(fontFamily: 'FontAwesome-Regular');
  static const fontAwesomeSolidFont =
      TextStyle(fontFamily: 'FontAwesome-Solids');

  static const themeColor = Color(0xFF111513);
  static const surfaceColor = Color(0xFF191E1B);
  static const surfaceElevated = Color(0xFF222923);
  static const surfaceSoft = Color(0xFF2C352E);
  static const surfaceHighlight = Color(0xFF354137);
  static const primaryColor1 = Color(0xFFF3F7EF);
  static const primaryColor2 = Color(0xFFB8C4B8);
  static const accentColor1 = Color(0xFFE1F7B6);
  static const accentColor1light = Color(0xFFF0FFD6);
  static const accentColor2 = Color(0xFFBFE98D);
  static const successColor = Color(0xFFBFE98D);
  static const warningColor = Color(0xFFF4C96B);
  static const errorColor = Color(0xFFFF938A);

  static const cornerSmall = 12.0;
  static const cornerMedium = 18.0;
  static const cornerLarge = 26.0;
  static const motionCurve = Curves.easeOutCubic;
  static const motionFast = Duration(milliseconds: 180);
  static const motionStandard = Duration(milliseconds: 260);

  ThemeData get defaultThemeData {
    final scheme = ColorScheme.fromSeed(
      seedColor: accentColor2,
      brightness: Brightness.dark,
      surface: themeColor,
      primary: accentColor2,
      secondary: accentColor1,
      tertiary: surfaceHighlight,
      onPrimary: const Color(0xFF182014),
      onSecondary: const Color(0xFF182014),
      onSurface: primaryColor1,
      error: errorColor,
    );

    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      scaffoldBackgroundColor: themeColor,
      dialogBackgroundColor: surfaceElevated,
      canvasColor: themeColor,
      fontFamily: 'Unageo',
      colorScheme: scheme,
      primaryColor: accentColor2,
      primaryColorDark: accentColor2,
      splashFactory: InkSparkle.splashFactory,
      visualDensity: VisualDensity.standard,
      iconTheme: const IconThemeData(color: primaryColor1, size: 22),
      appBarTheme: const AppBarTheme(
        backgroundColor: Colors.transparent,
        foregroundColor: primaryColor1,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        centerTitle: false,
        titleTextStyle: TextStyle(
          color: primaryColor1,
          fontFamily: 'Unageo',
          fontSize: 22,
          fontWeight: FontWeight.w800,
          letterSpacing: -0.4,
        ),
      ),
      cardTheme: const CardThemeData(
        color: surfaceColor,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.all(Radius.circular(cornerMedium)),
        ),
      ),
      listTileTheme: ListTileThemeData(
        dense: true,
        minLeadingWidth: 0,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 5),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(cornerMedium),
        ),
        iconColor: primaryColor2,
        textColor: primaryColor1,
        subtitleTextStyle: secondoryTextStyle.copyWith(
          color: primaryColor2,
          fontSize: 12,
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: accentColor2,
          foregroundColor: const Color(0xFF182014),
          elevation: 0,
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(cornerMedium),
          ),
          textStyle: secondoryTextStyleMedium.copyWith(fontSize: 14),
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: accentColor2,
          foregroundColor: const Color(0xFF182014),
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(cornerMedium),
          ),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: accentColor1,
          side: const BorderSide(color: surfaceHighlight),
          padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 13),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(cornerMedium),
          ),
        ),
      ),
      chipTheme: ChipThemeData(
        backgroundColor: surfaceElevated,
        selectedColor: accentColor2,
        disabledColor: surfaceSoft,
        labelStyle: secondoryTextStyleMedium.copyWith(
          color: primaryColor1,
          fontSize: 12,
        ),
        secondaryLabelStyle: secondoryTextStyleMedium.copyWith(
          color: const Color(0xFF182014),
        ),
        side: BorderSide.none,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(999),
        ),
      ),
      bottomSheetTheme: const BottomSheetThemeData(
        backgroundColor: surfaceElevated,
        modalBackgroundColor: surfaceElevated,
        surfaceTintColor: Colors.transparent,
        showDragHandle: true,
        dragHandleColor: surfaceHighlight,
        shape: RoundedRectangleBorder(
          borderRadius:
              BorderRadius.vertical(top: Radius.circular(cornerLarge)),
        ),
      ),
      dialogTheme: const DialogThemeData(
        backgroundColor: surfaceElevated,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.all(Radius.circular(cornerLarge)),
        ),
      ),
      pageTransitionsTheme: const PageTransitionsTheme(
        builders: {
          TargetPlatform.android: FadeForwardsPageTransitionsBuilder(),
          TargetPlatform.iOS: FadeForwardsPageTransitionsBuilder(),
          TargetPlatform.linux: FadeForwardsPageTransitionsBuilder(),
          TargetPlatform.macOS: FadeForwardsPageTransitionsBuilder(),
          TargetPlatform.windows: FadeForwardsPageTransitionsBuilder(),
        },
      ),
      scrollbarTheme: ScrollbarThemeData(
        thumbColor: WidgetStatePropertyAll(accentColor2.withValues(alpha: .72)),
        trackColor: WidgetStatePropertyAll(surfaceSoft.withValues(alpha: .35)),
        interactive: true,
        radius: const Radius.circular(12),
        thickness: const WidgetStatePropertyAll(4),
      ),
      progressIndicatorTheme: const ProgressIndicatorThemeData(
        color: accentColor2,
        linearTrackColor: surfaceSoft,
        circularTrackColor: surfaceSoft,
      ),
      textSelectionTheme: const TextSelectionThemeData(
        cursorColor: accentColor2,
        selectionColor: Color(0x668DBB61),
        selectionHandleColor: accentColor2,
      ),
      switchTheme: SwitchThemeData(
        thumbColor: const WidgetStatePropertyAll(primaryColor1),
        trackColor: WidgetStateProperty.resolveWith((states) =>
            states.contains(WidgetState.selected) ? accentColor2 : surfaceSoft),
        trackOutlineColor: const WidgetStatePropertyAll(Colors.transparent),
      ),
      searchBarTheme: SearchBarThemeData(
        backgroundColor: const WidgetStatePropertyAll(surfaceElevated),
        surfaceTintColor: const WidgetStatePropertyAll(Colors.transparent),
        elevation: const WidgetStatePropertyAll(0),
        padding: const WidgetStatePropertyAll(
          EdgeInsets.symmetric(horizontal: 18),
        ),
        shape: WidgetStatePropertyAll(
          RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(cornerLarge),
            side: const BorderSide(color: surfaceHighlight),
          ),
        ),
        hintStyle: const WidgetStatePropertyAll(
          TextStyle(color: primaryColor2),
        ),
      ),
      popupMenuTheme: const PopupMenuThemeData(
        color: surfaceElevated,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.all(Radius.circular(cornerMedium)),
        ),
        textStyle: TextStyle(color: primaryColor1),
      ),
      navigationBarTheme: const NavigationBarThemeData(
        backgroundColor: surfaceElevated,
        indicatorColor: accentColor2,
        surfaceTintColor: Colors.transparent,
        height: 72,
        labelTextStyle: WidgetStatePropertyAll(
          TextStyle(fontFamily: 'Gilroy', fontWeight: FontWeight.w700),
        ),
      ),
    );
  }
}

// Backward-compatible alias used throughout the existing client.
// ignore: camel_case_types
typedef Default_Theme = AppTheme;
