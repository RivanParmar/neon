    .data
    .p2align 3

// Define arrays of floating-point constants required for sepia color transformation.
// Each array consists of four single-precision floating-point values (4S) to fully
// occupy a 128-bit NEON quad-word register (Qn). The final 0.0 in each array ensures
// the complete 128-bit register is initialized, aligning with typical vector load patterns,
// even if that specific lane is not directly used in the color calculation formulas.
SEPIA_R_ROW_CONST: .float 0.393, 0.769, 0.189, 0.0
SEPIA_G_ROW_CONST: .float 0.349, 0.686, 0.168, 0.0
SEPIA_B_ROW_CONST: .float 0.272, 0.534, 0.131, 0.0

FP_ZERO_S_CONST: .float 0.0
FP_255_S_CONST: .float 255.0

    .text
    .global sepia
    .type sepia, %function
    .p2align 4

// Function signature (parameters passed in registers):
// X0: Pixels pointer (uint8_t* pixels) - Base address of the bitmap pixel data
// W1 (or X1): Width of bitmap (uint32_t width) - Number of pixels per row
// W2 (or X2): Height of bitmap (uint32_t height) - Number of rows
// W3 (or X3): Stride (Bytes per row) (uint32_t stride) - Total bytes to move to the next row

sepia:
    STP FP, LR, [SP, #-16]!

    MOV FP, SP

    STP X19, X20, [SP, #-16]!

    STP Q8, Q9, [SP, #-32]!
    STP Q10, Q11, [SP, #-32]!
    STP Q12, Q13, [SP, #-32]!
    STP Q14, Q15, [SP, #-32]!

    MOV X19, X0
    MOV X20, #0

    // Load all the constants
    LDR Q13, SEPIA_R_ROW_CONST
    LDR Q14, SEPIA_G_ROW_CONST
    LDR Q15, SEPIA_B_ROW_CONST

    LDR S16, FP_ZERO_S_CONST
    LDR S17, FP_255_S_CONST

    FMOV W20, S16
    DUP V18.4S, W20             // Duplicate 0.0 across all single-precision floating-point words

    FMOV W21, S17
    DUP V19.4S, W21             // Duplicate 255.0 across all single-precision floating-point words

loop_rows:
    CBZ W2, exit_sepia
    ADD X0, X19, X20

    LSR W4, W1, #3
    CBZ W4, process_remaining_pixels

vectors:
    // For the sepia filter we can only load 8 pixels at a time due to limited number of registers.
    LD4 { V0.8B, V1.8B, V2.8B, V3.8B }, [X0], #32

    // Perform the sepia calculation on the RGB values. Alpha values remain unchanged.
    // First we convert the bytes of the pixels to single-precision floating-point words.
    UXTL V4.8H, V0.8B          // Extend the unsigned byte to half-word
    UXTL V20.4S, V4.4H         // Extend the unsigned lower half-word to float word
    UXTL2 V21.4S, V4.8H        // Extend the unsigned upper half-word to float word
    UCVTF V20.4S, V20.4S       // Convert the extended integers to floats
    UCVTF V21.4S, V21.4S

    UXTL V5.8H, V1.8B
    UXTL V22.4S, V5.4H
    UXTL2 V23.4S, V5.8H
    UCVTF V22.4S, V22.4S
    UCVTF V23.4S, V23.4S

    UXTL V6.8H, V2.8B
    UXTL V24.4S, V6.4H
    UXTL2 V25.4S, V6.8H
    UCVTF V24.4S, V24.4S
    UCVTF V25.4S, V25.4S

    // Keep a copy of the values for calculation
    MOV V7.4S, V20.4S
    MOV V8.4S, V21.4S
    MOV V9.4S, V22.4S
    MOV V10.4S, V23.4S
    MOV V11.4S, V24.4S
    MOV V12.4S, V25.4S

    // Calculate the RGB values using the following formulas:
    // sepiaRed = (red * 0.393) + (green * 0.769) + (blue * 0.189)
    // sepiaGreen = (red * 0.349) + (green * 0.686) + (blue * 0.168)
    // sepiaBlue = (red * 0.272) + (green * 0.534) + (blue * 0.131)
    FMUL V20.4S, V7.4S, V13.S[0]
    FMUL V21.4S, V8.4S, V13.S[0]
    FMLA V20.4S, V9.4S, V13.S[1]
    FMLA V21.4S, V10.4S, V13.S[1]
    FMLA V20.4S, V11.4S, V13.S[2]
    FMLA V21.4S, V12.4S, V13.S[2]

    FMUL V22.4S, V7.4S, V14.S[0]
    FMUL V23.4S, V8.4S, V14.S[0]
    FMLA V22.4S, V9.4S, V14.S[1]
    FMLA V23.4S, V10.4S, V14.S[1]
    FMLA V22.4S, V11.4S, V14.S[2]
    FMLA V23.4S, V12.4S, V14.S[2]

    FMUL V24.4S, V7.4S, V15.S[0]
    FMUL V25.4S, V8.4S, V15.S[0]
    FMLA V24.4S, V9.4S, V15.S[1]
    FMLA V25.4S, V10.4S, V15.S[1]
    FMLA V24.4S, V11.4S, V15.S[2]
    FMLA V25.4S, V12.4S, V15.S[2]

    // Clamp the calculated values to the range of 0 to 255
    FMAX V20.4S, V20.4S, V18.4S
    FMIN V20.4S, V20.4S, V19.4S
    FMAX V21.4S, V21.4S, V18.4S
    FMIN V21.4S, V21.4S, V19.4S
    FMAX V22.4S, V22.4S, V18.4S
    FMIN V22.4S, V22.4S, V19.4S
    FMAX V23.4S, V23.4S, V18.4S
    FMIN V23.4S, V23.4S, V19.4S
    FMAX V24.4S, V24.4S, V18.4S
    FMIN V24.4S, V24.4S, V19.4S
    FMAX V25.4S, V25.4S, V18.4S
    FMIN V25.4S, V25.4S, V19.4S

    // Convert the single-precision floating-point words back to bytes.
    FCVTNS V20.4S, V20.4S      // Convert the floats back into signed integers
    FCVTNS V21.4S, V21.4S
    UQXTN V4.4H, V20.4S        // Narrow down the floating-point words to half-words
    UQXTN V5.4H, V21.4S
    MOV V4.D[1], V5.D[0]       // Move the value in V5 to the upper half of V4
    UQXTN V0.8B, V4.8H         // Narrow down the half-word back to byte

    FCVTNS V22.4S, V22.4S
    FCVTNS V23.4S, V23.4S
    UQXTN V5.4H, V22.4S
    UQXTN V6.4H, V23.4S
    MOV V5.D[1], V6.D[0]
    UQXTN V1.8B, V5.8H

    FCVTNS V24.4S, V24.4S
    FCVTNS V25.4S, V25.4S
    UQXTN V6.4H, V24.4S
    UQXTN V4.4H, V25.4S
    MOV V6.D[1], V4.D[0]
    UQXTN V2.8B, V6.8H

    SUB X9, X0, #32
    ST4 { V0.8B, V1.8B, V2.8B, V3.8B }, [X9]

    SUBS W4, W4, #1
    B.NE vectors

process_remaining_pixels:
    ANDS W4, W1, #7
    CBZ W4, end_row_processing

singles:
    LDR W8, [X0]

    AND W9, W8, #0xFF
    LSR W10, W8, #8
    AND W10, W10, #0xFF
    LSR W11, W8, #16
    AND W11, W11, #0xFF
    LSR W12, W8, #24

    // Perform sepia calculation.
    // Directly convert the RGB values to floats.
    UCVTF S21, W9
    UCVTF S22, W10
    UCVTF S23, W11

    // Keep a copy of the values for calculation.
    FMOV S24, S21
    FMOV S25, S22
    FMOV S26, S23

    // Calculate the RGB values using the same formulas as before.
    FMUL S21, S24, V13.S[0]
    FMLA S21, S25, V13.S[1]
    FMLA S21, S26, V13.S[2]

    FMUL S22, S24, V14.S[0]
    FMLA S22, S25, V14.S[1]
    FMLA S22, S26, V14.S[2]

    FMUL S23, S24, V15.S[0]
    FMLA S23, S25, V15.S[1]
    FMLA S23, S26, V15.S[2]

    // Clamp the values to the range of 0 to 255.
    FMAX S21, S21, S16
    FMIN S21, S21, S17
    FMAX S22, S22, S16
    FMIN S22, S22, S17
    FMAX S23, S23, S16
    FMIN S23, S23, S17

    // Convert the floats back to integers.
    FCVTNS W9, S21
    FCVTNS W10, S22
    FCVTNS W11, S23

    LSL W8, W12, #24
    LSL W11, W11, #16
    ORR W8, W8, W11
    LSL W10, W10, #8
    ORR W8, W8, W10
    ORR W8, W8, W9

    STR W8, [X0]

    ADD X0, X0, #4

    SUBS W4, W4, #1
    B.NE singles

end_row_processing:
    ADD X20, X20, X3

    SUBS W2, W2, #1
    B.NE loop_rows

exit_sepia:
    LDP Q14, Q15, [SP], #32
    LDP Q12, Q13, [SP], #32
    LDP Q10, Q11, [SP], #32
    LDP Q8, Q9, [SP], #32

    LDP X19, X20, [SP], #16

    LDP FP, LR, [SP], #16

    RET