# Add project specific ProGuard rules here.
# QuickJS Executor Module

# Keep QuickJS classes
-keep class app.cash.quickjs.** { *; }

# Keep module public API
-keep public class com.xiaomo.quickjs.** { *; }
