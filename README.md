# Neon
This is an Android application built with Java that demonstrates the use of **Arm Assembly** (leveraging **Arm® Neon™** SIMD technology) for high-performance image processing filters. C++ acts as middleware, bridging the Java application layer and the low-level Assembly implementations.


# ✨ Features
- Apply different filters such as **Grayscale**, **Invert**, **Brightness**, **Contrast** and **Sepia**.
- Modify parameters for filters like the amount of brightness to be applied or the amount of contrast to be applied.
- Switch between **Java** and **Assembly** implementations for each filter to observe differences.
- **Measure and Compare** the performance of filter applications between Java and Assembly.
- Save the filtered images.

# ⚒️ Building
You can try out the app by downloading it directly from the [Releases section]().

To build the app from source:
1. Clone the repository.
2. Open the project in Android Studio[^1].
3. Build and Run.

**Note**: An AArch64 (Arm®v8-A 64-bit) device or emulator is needed to run the app.

# 📖 Assembly Implementation Details
For in-depth documentation on how each filter has been implemented using Arm Assembly, please refer to the [assembly implementation doc]().

# 💡 Potential Future Filters
A list of potential filters that can be implemented using Arm Assembly include:
- Box Blur
- Edge Detection
- Sharpen
- Threshold
- Gaussian

# 📚 Helpful Resources
Here are some useful resources to learn Arm Assembly:
- [Link 1]()
- [Link 2]()

# License
This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

[^1]: Android Studio LadyBug or newer is recommended with **Android NDK** and **CMake** installed.
