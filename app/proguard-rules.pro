# Keep the code's own names and line numbers in the Play build, so a crash report reads
# plainly (the app is small, and shrinking still removes what isn't used).
-dontobfuscate
-keepattributes SourceFile,LineNumberTable
