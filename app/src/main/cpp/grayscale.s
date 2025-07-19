    .data
    .p2align  3

FP_ZERO_S_CONST: .float 0.0
FP_255_S_CONST:  .float 255.0

    .text
    .global   grayscale
    .type     grayscale, %function
    .p2align  4

// Function signature (parameters passed in registers):
// X0: Pixels pointer (uint8_t* pixels) - Base address of the bitmap pixel data
// W1 (or X1): Width of bitmap (uint32_t width) - Number of pixels per row
// W2 (or X2): Height of bitmap (uint32_t height) - Number of rows
// W3 (or X3): Stride (Bytes per row) (uint32_t stride) - Total bytes to move to the next row
// S0: Red coefficient (float redCoefficient)
// S1: Green coefficient (float greenCoefficient)
// S2: Blue coefficient (float blueCoefficient)

grayscale:
    STP FP, LR, [SP, #-16]!

    MOV FP, SP

    STP X19, X20, [SP, #-16]!

    STP Q8, Q9, [SP, #-32]!
    STP Q10, Q11, [SP, #-32]!

    MOV X19, X0
    MOV X20, #0

    // Move the coefficients into different registers to avoid overwriting them during
    // the loading of pixel data
    FMOV S8, S0
    FMOV S9, S1
    FMOV S10, S2

    // Load both the constants
    LDR S11, FP_ZERO_S_CONST
    LDR S12, FP_255_S_CONST

    // Duplicate the required values across different registers
    FMOV W21, S8
    DUP V8.4S, W21

    FMOV W22, S9
    DUP V9.4S, W22

    FMOV W23, S10
    DUP V10.4S, W23

    FMOV W24, S11
    DUP V11.4S, W24

    FMOV W25, S12
    DUP V12.4S, W25

loop_rows:
    CBZ W2, exit_grayscale
    ADD X0, X19, X20

    LSR W4, W1, #4  // Divide width by 16 (2^4)
    CBZ W4, process_remaining_pixels

vectors:
    LD4 { V0.8B, V1.8B, V2.8B, V3.8B }, [X0], #32

    LD4 { V4.8B, V5.8B, V6.8B, V7.8B }, [X0], #32

    // Perform the grayscale calculation on the RGB values. Alpha values remain unchanged.
    // We will use the formula: redCoefficient * red + greenCoefficient * green + blueCoefficient * blue
    // to calculate the gray value.
    // First we convert the bytes of the pixels to single-precision floating-point words.
    UXTL V13.8H, V0.8B         // Extend the unsigned byte to half-word
    UXTL V20.4S, V13.4H        // Extend the unsigned lower half-word to float word
    UXTL2 V21.4S, V13.8H       // Extend the unsigned upper half-word to float word
    UCVTF V20.4S, V20.4S       // Convert the extended integers to floats
    UCVTF V21.4S, V21.4S

    UXTL V14.8H, V1.8B
    UXTL V22.4S, V14.4H
    UXTL2 V23.4S, V14.8H
    UCVTF V22.4S, V22.4S
    UCVTF V23.4S, V23.4S

    UXTL V15.8H, V2.8B
    UXTL V24.4S, V15.4H
    UXTL2 V25.4S, V15.8H
    UCVTF V24.4S, V24.4S
    UCVTF V25.4S, V25.4S

    UXTL V13.8H, V4.8B
    FMUL V20.4S, V20.4S, V8.4S   // We will also start calculating the values
    UXTL V26.4S, V13.4H
    UXTL2 V27.4S, V13.8H
    FMLA V20.4S, V22.4S, V9.4S
    UCVTF V26.4S, V26.4S
    UCVTF V27.4S, V27.4S

    UXTL V14.8H, V5.8B
    FMLA V20.4S, V24.4S, V10.4S
    UXTL V28.4S, V14.4H
    UXTL2 V29.4S, V14.8H
    FMUL V21.4S, V21.4S, V8.4S
    UCVTF V28.4S, V28.4S
    UCVTF V29.4S, V29.4S

    UXTL V15.8H, V6.8B
    FMLA V21.4S, V23.4S, V9.4S
    UXTL V30.4S, V15.4H
    UXTL2 V31.4S, V15.8H
    FMLA V21.4S, V25.4S, V10.4S
    UCVTF V30.4S, V30.4S
    UCVTF V31.4S, V31.4S

    FMUL V26.4S, V26.4S, V8.4S
    FMLA V26.4S, V28.4S, V9.4S
    FMLA V26.4S, V30.4S, V10.4S

    FMUL V27.4S, V27.4S, V8.4S
    FMLA V27.4S, V29.4S, V9.4S
    FMLA V27.4S, V31.4S, V10.4S

    // Clamp the calculated values to the range of 0 to 255
    FMAX V20.4S, V20.4S, V11.4S
    FMIN V20.4S, V20.4S, V12.4S
    FMAX V21.4S, V21.4S, V11.4S
    FMIN V21.4S, V21.4S, V12.4S

    FMAX V26.4S, V26.4S, V11.4S
    FMIN V26.4S, V26.4S, V12.4S
    FMAX V27.4S, V27.4S, V11.4S
    FMIN V27.4S, V27.4S, V12.4S

    // Convert the single-precision floating-point words back to byte.
    FCVTNS V20.4S, V20.4S      // Convert the floats back into signed integers
    FCVTNS V21.4S, V21.4S
    SQXTN V13.4H, V20.4S       // Narrow down the floating-point words to half-words (signed)
    SQXTN V14.4H, V21.4S
    MOV V13.D[1], V14.D[0]     // Move the value in V14 to the upper half of V13
    UQXTN V0.8B, V13.8H        // Narrow down the half-word back to byte

    FCVTNS V26.4S, V26.4S
    FCVTNS V27.4S, V27.4S
    SQXTN V15.4H, V26.4S
    SQXTN V13.4H, V27.4S
    MOV V15.D[1], V13.D[0]
    UQXTN V4.8B, V15.8H

    // V0 contains the gray value common to V0, V1 and V2
    MOV V1.8B, V0.8B
    MOV V2.8B, V0.8B

    // V4 contains the gray value common to V4, V5 and V6
    MOV V5.8B, V4.8B
    MOV V6.8B, V4.8B

    SUB X9, X0, #64
    ST4 { V0.8B, V1.8B, V2.8B, V3.8B }, [X9]
    ADD X9, X9, #32
    ST4 { V4.8B, V5.8B, V6.8B, V7.8B }, [X9]

    SUBS W4, W4, #1
    B.NE vectors

process_remaining_pixels:
    ANDS W4, W1, #15  // Find the remainder when divided by 16
    B.EQ end_row_processing

    LSR W5, W4, #3
    CBZ W5, singles_remainder

vectors_remainder:
    LD4 { V0.8B, V1.8B, V2.8B, V3.8B }, [X0], #32

    // Perform grayscale calculation
    UXTL V24.8H, V0.8B
    UXTL V26.4S, V24.4H
    UXTL2 V27.4S, V24.8H
    UCVTF V26.4S, V26.4S
    UCVTF V27.4S, V27.4S

    UXTL V25.8H, V1.8B
    UXTL V28.4S, V25.4H
    UXTL2 V29.4S, V25.8H
    UCVTF V28.4S, V28.4S
    UCVTF V29.4S, V29.4S

    UXTL V13.8H, V2.8B
    UXTL V30.4S, V13.4H
    UXTL2 V31.4S, V13.8H
    UCVTF V30.4S, V30.4S
    UCVTF V31.4S, V31.4S

    FMUL V26.4S, V26.4S, V8.4S
    FMLA V26.4S, V28.4S, V9.4S
    FMLA V26.4S, V30.4S, V10.4S

    FMUL V27.4S, V27.4S, V8.4S
    FMLA V27.4S, V29.4S, V9.4S
    FMLA V27.4S, V31.4S, V10.4S

    FMAX V26.4S, V26.4S, V11.4S
    FMIN V26.4S, V26.4S, V12.4S
    FMAX V27.4S, V27.4S, V11.4S
    FMIN V27.4S, V27.4S, V12.4S

    FCVTNS V26.4S, V26.4S
    FCVTNS V27.4S, V27.4S
    SQXTN V24.4H, V26.4S
    SQXTN V25.4H, V27.4S
    MOV V24.D[1], V25.D[0]
    UQXTN V0.8B, V24.8H

    MOV V1.8B, V0.8B
    MOV V2.8B, V0.8B

    SUB X9, X0, #32
    ST4 { V0.8B, V1.8B, V2.8B, V3.8B }, [X9]

    SUB W4, W4, #8
    CBZ W4, end_row_processing

singles_remainder:
    CBZ W4, end_row_processing

singles:
    LDR W8, [X0]

    AND W9, W8, #0xFF
    LSR W10, W8, #8
    AND W10, W10, #0xFF
    LSR W11, W8, #16
    AND W11, W11, #0xFF
    LSR W12, W8, #24

    // Perform grayscale calculation.
    // Directly convert the RGB values to floats.
    UCVTF S21, W9
    UCVTF S22, W10
    UCVTF S23, W11

    // Calculate the gray value as before.
    FMUL S24, S21, S8

    FMUL S25, S22, S9
    FADD S24, S24, S25

    FMUL S25, S23, S10
    FADD S24, S24, S25

    // Clamp the gray value to the range of 0 to 255.
    FMAX S24, S24, S11
    FMIN S24, S24, S12

    // Convert the float back to integer
    FCVTNS W13, S24
    AND W13, W13, #0xFF

    ORR W8, W13, W13, LSL #8
    ORR W8, W8, W13, LSL #16
    ORR W8, W8, W12, LSL #24

    STR W8, [X0]

    ADD X0, X0, #4

    SUBS W4, W4, #1
    B.NE singles

end_row_processing:
    ADD X20, X20, X3

    SUBS W2, W2, #1
    B.NE loop_rows

exit_grayscale:
    LDP Q10, Q11, [SP], #32
    LDP Q8, Q9, [SP], #32

    LDP X19, X20, [SP], #16

    LDP FP, LR, [SP], #16

    RET