import 'package:ft_music/blocs/player_overlay/player_overlay_cubit.dart';
import 'package:ft_music/screens/widgets/player_overlay_wrapper.dart';
import 'package:ft_music/screens/widgets/mini_player_widget.dart';
import 'package:ft_music/core/theme/app_theme.dart';
import 'package:ft_music/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import 'package:responsive_framework/responsive_framework.dart';

class GlobalFooter extends StatelessWidget {
  const GlobalFooter({super.key, required this.navigationShell});
  final StatefulNavigationShell navigationShell;

  @override
  Widget build(BuildContext context) {
    context.watch<PlayerOverlayCubit>();
    final isMobile = ResponsiveBreakpoints.of(context).isMobile;

    return PlayerOverlayWrapper(
      child: BackButtonListener(
        // FIX H-04: Back button priority order:
        // ① Navigator routes (FullscreenLyricsView, PlayerSettings, TimerView, etc.)
        // ② UpNext panel collapse
        // ③ Player overlay hide
        // ④ GoRouter shell navigation
        // ⑤ System exit
        //
        // Previously the handler short-circuited at step ③ whenever the player
        // was visible, swallowing Navigator pops and causing sub-screens to
        // appear orphaned over a hidden/collapsed player.
        onBackButtonPressed: () async {
          final overlayC = context.read<PlayerOverlayCubit>();
          final router = GoRouter.of(context);

          // ① Navigator MUST have first priority — always.
          if (router.canPop()) {
            router.pop();
            return true;
          }

          // ② Collapse UpNext panel if expanded (player must be visible).
          if (overlayC.state && overlayC.collapseUpNextPanel()) {
            return true;
          }

          // ③ Hide the player overlay.
          if (overlayC.state) {
            overlayC.hidePlayer();
            return true;
          }

          // ④ Let PopScope handle tab/exit navigation below.
          return false;
        },
        child: PopScope(
          canPop: false,
          onPopInvokedWithResult: (didPop, _) async {
            if (didPop) return;
            await _handleHardwareBackPress(context);
          },
          child: Scaffold(
            backgroundColor: Default_Theme.themeColor,
            drawerScrimColor: Default_Theme.themeColor,
            body: isMobile
                ? _AnimatedPageView(navigationShell: navigationShell)
                : Row(
                    children: [
                      Padding(
                        padding: const EdgeInsets.only(left: 4),
                        child: VerticalNavBar(navigationShell: navigationShell),
                      ),
                      Expanded(
                        child:
                            _AnimatedPageView(navigationShell: navigationShell),
                      ),
                    ],
                  ),
            bottomNavigationBar: SafeArea(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.end,
                mainAxisSize: MainAxisSize.min,
                children: [
                  const MiniPlayerWidget(),
                  if (isMobile)
                    Container(
                      color: Colors.transparent,
                      margin: const EdgeInsets.symmetric(
                          vertical: 5, horizontal: 10),
                      child: HorizontalNavBar(navigationShell: navigationShell),
                    ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  /// Handles PopScope back presses using the same priority order as
  /// BackButtonListener above.
  Future<void> _handleHardwareBackPress(BuildContext context) async {
    final overlayC = context.read<PlayerOverlayCubit>();
    final router = GoRouter.of(context);

    // ① Navigator routes first
    if (router.canPop()) {
      router.pop();
      return;
    }

    // ② Collapse UpNext panel
    if (overlayC.state && overlayC.collapseUpNextPanel()) return;

    // ③ Hide player
    if (overlayC.state) {
      overlayC.hidePlayer();
      return;
    }

    // ④ Navigate to home tab
    if (navigationShell.currentIndex != 0) {
      navigationShell.goBranch(0);
      return;
    }

    // ⑤ Exit app
    if (context.mounted) {
      await SystemNavigator.pop();
    }
  }
}

class _AnimatedPageView extends StatefulWidget {
  const _AnimatedPageView({required this.navigationShell});
  final StatefulNavigationShell navigationShell;

  @override
  State<_AnimatedPageView> createState() => _AnimatedPageViewState();
}

class _AnimatedPageViewState extends State<_AnimatedPageView>
    with SingleTickerProviderStateMixin {
  late final AnimationController _animationController;
  late final Animation<double> _fadeAnimation;
  late final Animation<double> _scaleAnimation;
  int _previousIndex = 0;

  @override
  void initState() {
    super.initState();
    _previousIndex = widget.navigationShell.currentIndex;

    _animationController = AnimationController(
      duration: const Duration(milliseconds: 250),
      vsync: this,
    );

    _fadeAnimation = CurvedAnimation(
      parent: _animationController,
      curve: Curves.easeOutCubic,
    );

    _scaleAnimation = Tween<double>(begin: 0.96, end: 1.0).animate(
      CurvedAnimation(
        parent: _animationController,
        curve: Curves.easeOutCubic,
      ),
    );

    _animationController.forward();
  }

  @override
  void didUpdateWidget(_AnimatedPageView oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.navigationShell.currentIndex != _previousIndex) {
      _previousIndex = widget.navigationShell.currentIndex;
      _animationController.forward(from: 0.0);
    }
  }

  @override
  void dispose() {
    _animationController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return FadeTransition(
      opacity: _fadeAnimation,
      child: ScaleTransition(
        scale: _scaleAnimation,
        child: widget.navigationShell,
      ),
    );
  }
}

class VerticalNavBar extends StatelessWidget {
  const VerticalNavBar({super.key, required this.navigationShell});
  final StatefulNavigationShell navigationShell;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    return NavigationRail(
      backgroundColor: Default_Theme.themeColor.withValues(alpha: 0.3),
      destinations: [
        NavigationRailDestination(
            icon: const Icon(Icons.home_rounded), label: Text(l10n.navHome)),
        NavigationRailDestination(
            icon: const Icon(Icons.library_music_rounded),
            label: Text(l10n.navLibrary)),
        NavigationRailDestination(
            icon: const Icon(Icons.search_rounded),
            label: Text(l10n.navSearch)),
        NavigationRailDestination(
            icon: const Icon(Icons.queue_music_rounded),
            label: Text(l10n.navLocal)),
        NavigationRailDestination(
            icon: const Icon(Icons.download_rounded),
            label: Text(l10n.navOffline)),
      ],
      selectedIndex: navigationShell.currentIndex,
      minWidth: 70,
      onDestinationSelected: navigationShell.goBranch,
      groupAlignment: 0.0,
      unselectedIconTheme:
          const IconThemeData(color: Default_Theme.primaryColor2),
      indicatorColor: Default_Theme.accentColor2,
      indicatorShape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(15)),
      ),
    );
  }
}

class HorizontalNavBar extends StatelessWidget {
  const HorizontalNavBar({super.key, required this.navigationShell});
  final StatefulNavigationShell navigationShell;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final destinations = [
      (Icons.home_rounded, l10n.navHome),
      (Icons.library_music_rounded, l10n.navLibrary),
      (Icons.search_rounded, l10n.navSearch),
      (Icons.queue_music_rounded, l10n.navLocal),
      (Icons.download_rounded, l10n.navOffline),
    ];

    return Container(
      height: 68,
      padding: const EdgeInsets.all(6),
      decoration: BoxDecoration(
        color: Default_Theme.surfaceElevated,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(
          color: Default_Theme.primaryColor1.withValues(alpha: .06),
        ),
        boxShadow: const [
          BoxShadow(
            color: Color(0x55000000),
            blurRadius: 24,
            offset: Offset(0, 8),
          ),
        ],
      ),
      child: Row(
        children: [
          for (var index = 0; index < destinations.length; index++)
            Expanded(
              child: _BottomNavItem(
                icon: destinations[index].$1,
                label: destinations[index].$2,
                selected: navigationShell.currentIndex == index,
                onTap: () => navigationShell.goBranch(index),
              ),
            ),
        ],
      ),
    );
  }
}

class _BottomNavItem extends StatelessWidget {
  const _BottomNavItem({
    required this.icon,
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final color = selected
        ? const Color(0xFF182014)
        : Default_Theme.primaryColor2.withValues(alpha: .82);
    return Semantics(
      button: true,
      selected: selected,
      label: label,
      child: InkWell(
        borderRadius: BorderRadius.circular(18),
        onTap: onTap,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 180),
          curve: Curves.easeOutCubic,
          margin: const EdgeInsets.symmetric(horizontal: 2),
          decoration: BoxDecoration(
            color: selected ? Default_Theme.accentColor2 : Colors.transparent,
            borderRadius: BorderRadius.circular(18),
          ),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(icon, color: color, size: selected ? 22 : 21),
              const SizedBox(height: 3),
              Text(
                label,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  color: color,
                  fontFamily: 'Gilroy',
                  fontSize: 10,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
