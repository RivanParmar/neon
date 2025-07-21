# Assembly Filter Implementation Documentation
This document provides a detailed overview of the common architectural patterns, register usage conventions, and data processing techniques used across the Arm® Assembly filter implementations in this project.

# Base Assembly Filter Template
Most Assembly filter functions in this project follow a similar structure. Below is a simplified template illustrating the common prologue, main loops and epilogue. Specific filter logic would reside within the `vectors`, `vectors_remainder` and `singles` sections.

```gas
    .data
    .p2align 3
    // Common constants if needed, e.g., FP_ZERO_CONST, FP_255_CONST

    .text
    .global filter_function_name
    .type filter_function_name, %function
    .p2align 4

// Function Signature (parameters passed in registers following AArch64 PCS):
// X0: Pixels pointer (uint8_t* pixels)
// W1 (or X1): Width of bitmap (uint32_t width)
// W2 (or X2): Height of bitmap (uint32_t height)
// W3 (or X3): Stride (Bytes per row) (uint32_t stride)
// ... additional filter-specific parameters

filter_function_name:
    // --- Prologue: Function Setup ---
    STP FP, LR, [SP, #-16]!      // Save Frame Pointer (FP) and Link Register (LR)
    MOV FP, SP                   // Set current Frame Pointer
    STP X19, X20, [SP, #-16]!    // Save callee-saved general-purpose registers
    STP Q8, Q9, [SP, #-32]!      // Save callee-saved SIMD registers (Q8-Q15 if used)

    // --- Initialization ---
    MOV X19, X0                  // Save base pixel pointer (X0 is volatile)
    MOV X20, #0                  // Initialize row offset

// --- Main Row Loop ---
loop_rows:
    CBZ W2, exit_filter_function_name  // If height (W2) is 0, exit
    ADD X0, X19, X20                   // Calculate start address of current row

     // Calculate number of 16-pixel chunks for current row
    LSR W4, W1, #4                    // W4 = width / 16
    CBZ W4, process_remaining_pixels  // If no full 16-pixel chunks, skip to remainder

vectors:
    // Load 16 pixels (64 bytes) of RGBA data, interleaving into V0-V7
    LD4 { V0.8B, V1.8B, V2.8B, V3.8B }, [X0], #32  // Load 8 pixels (R0-7, G0-7, B0-7, A0-7)
    LD4 { V4.8B, V5.8B, V6.8B, V7.8B }, [X0], #32  // Load next 8 pixels (R8-15, G8-15, B8-15, A8-15)

    // --- Filter-Specific Calculations here (for 16 pixels) ---

    // Store 16 pixels (64 bytes) of RGBA data back to memory
    SUB X9, X0, #64                           // Calculate starting address for storing
    ST4 { V0.8B, V1.8B, V2.8B, V3.8B }, [X9]  // Store first 8 pixels
    ADD X9, X9, #32                           // Move pointer for next store
    ST4 { V4.8B, V5.8B, V6.8B, V7.8B }, [X9]  // Store next 8 pixels

    SUBS W4, W4, #1                 // Decrement 16-pixel chunk counter
    B.NE vectors                    // Loop if more chunks remain

// --- Remainder Processing ---
process_remaining_pixels:
    ANDS W4, W1, #15               // W4 = width % 16 (pixels remaining after 16-pixel chunks)
    B.EQ end_row_processing        // If no remainder, end row

    LSR W5, W4, #3                 // W5 = remainder / 8 (check for 8-pixel chunks)
    CBZ W5, singles_remainder      // If no full 8-pixel chunks, skip to singles

vectors_remainder:
    // Load 8 pixels (32 bytes) of RGBA data
    LD4 { V0.8B, V1.8B, V2.8B, V3.8B }, [X0], #32

    // --- Filter-Specific Calculations here (for 8 pixels) ---

    // Store 8 pixels (32 bytes) of RGBA data
    SUB X9, X0, #32                            // Calculate starting address for storing
    ST4 { V0.8B, V1.8B, V2.8B, V3.8B }, [X9]   // Store the 8 pixels

    SUB W4, W4, #8                  // Decrement 8-pixel chunk counter
    CBZ W4, end_row_processing      // If no remainder, end row

// --- Single Pixel Processing Loop ---
singles_remainder:
    CBZ W4, end_row_processing      // If no remaining singles, end row

singles:
    // Load single 32-bit pixel
    LDR W8, [X0]              // Load current pixel (RGBA)

    // Extract individual R, G, B, A components
    AND W9, W8, #0xFF         // W9 contains Red
    LSR W10, W8, #8           // W10 contains Green
    AND W10, W10, #0xFF
    LSR W11, W8, #16          // W11 contains Blue
    AND W11, W11, #0xFF
    LSR W12, W8, #24          // W12 contains Alpha

    // --- Filter-Specific Scalar Calculations here ---

    // Reconstruct pixel in the reverse order
    LSL W8, W12, #24
    LSL W11, W11, #16
    ORR W8, W8, W11
    LSL W10, W10, #8
    ORR W8, W8, W10
    ORR W8, W8, W9

    // Store single 32-bit pixel
    STR W8, [X0]

    ADD X0, X0, #4              // Move to next pixel (4 bytes)

    SUBS W4, W4, #1             // Decrement single pixel counter
    B.NE singles                // Loop if more singles remain

// --- End of Row Processing ---
end_row_processing:
    ADD X20, X20, X3           // Update row offset by stride

    SUBS W2, W2, #1            // Decrement row counter (height)
    B.NE loop_rows             // Loop if more rows remain

// --- Epilogue: Function Teardown ---
exit_filter_function_name:
    LDP Q8, Q9, [SP], #32      // Restore callee-saved SIMD/FP registers
                               // (Order is reverse of saving)

    LDP X19, X20, [SP], #16    // Restore callee-saved general-purpose registers
    LDP FP, LR, [SP], #16      // Restore Frame Pointer and Link Register

    RET                        // Return to caller
```

## Function Structure and Control Flow Explained
Referring to the template above:

### Standard Function Prologue and Epilogue
- **Prologue**: Every function starts by saving critical registers that the Arm® AArch64 Procedure Call Standard (PCS) dictates must be preserved across function calls (callee-saved registers). This includes the Frame Pointer (`FP`/`X29`), Link Register (`LR`/`X30`), and general-purpose registers (`X19`-`X30`), as well as Arm® NEON™ floating-point/vector registers (`V8`-`V15`). The `!` suffix on `STP` (Store Pair) indicates pre-indexed addressing, meaning the stack pointer (`SP`) is adjusted before the store.
- **Epilogue**: Before returning, the function restores the saved registers in the reverse order they were saved. The `!` suffix on `LDP` (Load Pair) indicates post-indexed addressing, where the `SP` is adjusted after the load. This correctly cleans up the stack frame.

### Main Processing Loop
The `loop_rows` is the outer loop that processes the bitmap row by row.
- **Loop Condition**: `CBZ W2, exit_filter_function_name` checks if the height (passed in `W2`) has reached zero. If it has, all rows are processed, and the function exits.
- **Row Pointer Update**: `ADD X0, X19, X20` calculates the memory address for the beginning of the current row. `X19` holds the unchanging base address of the entire pixel buffer, and `X20` accumulates the offset for the current row, which starts at 0 and increases by stride for each subsequent row.
- **Next Row Preparation**: At the end of processing each row (`end_row_processing`), `ADD X20, X20, X3` updates the row offset by adding the stride (bytes per row, stored in `X`3). `SUBS W2, W2, #1` decrements the row counter (height), and `B.NE loop_rows` branches back to the start of the `loop_rows` if there are more rows to process.

### Inner Pixel Processing Loops
Each row is optimized by processing pixels in large chunks using Arm® NEON™ vector instructions, falling back to smaller chunks or single pixels for the remainder.
- **Vectorized Processing** (`vectors` loop):
  - This is the primary Arm® NEON™ optimized section. It processes pixels in chunks of 16 (since RGBA_8888 is 4 bytes/pixel, 16 pixels are 64 bytes).
  - `LSR W4, W1, #4` calculates how many full 16-pixel chunks exist in the current row (width / 16).
  - `CBZ W4, process_remaining_pixels` jumps past this section if there are no full 16-pixel chunks.
  - The loop control `SUBS W4, W4, #1` and `B.NE vectors` continues until all 16-pixel chunks are processed.
- **Vector Remainder Processing** (`vectors_remainder` loop):
  - Handles any remaining pixels (width % 16) that couldn't be processed by the 16-pixel chunks. It first checks if there are enough pixels for an 8-pixel chunk.
  - `ANDS W4, W1, #15` gets the remainder of pixels after dividing width by 16.
  - `LSR W5, W4, #3` checks if this remainder is at least 8. If not, it skips to singles_remainder.
  - This loop isn't needed when the `vectors` loop is only processing 8 pixels at a time.
- **Single Pixel Processing** (`singles` loop):
  - This is the scalar fallback for any pixels (less than 8) that remain after vector processing. It processes pixels one by one.
  - `LDR W8, [X0]` loads a single 32-bit pixel. Individual byte components (R, G, B, A) are then extracted, processed, and reassembled using scalar instructions.
  - `STR W8, [X0]` stores the modified pixel.


 **Note**: The registers used in specific filter implementations may differ based on their complexity and the number of parameters/constants required.
