    .data
    .p2align 3

FP_ZERO_S_CONST: .float 0.0
FP_128_S_CONST: .float 128.0
FP_255_S_CONST: .float 255.0

    .text
    .global contrast
    .type contrast, %function
    .p2align 4

// Function signature (parameters passed in registers):
// X0: Pixels pointer (uint8_t* pixels) - Base address of the bitmap pixel data
// W1 (or X1): Width of bitmap (uint32_t width) - Number of pixels per row
// W2 (or X2): Height of bitmap (uint32_t height) - Number of rows
// W3 (or X3): Stride (Bytes per row) (uint32_t stride) - Total bytes to move to the next row
// S0: Contrast factor (float contrast) - Amount of contrast to be applied

contrast:
    STP FP, LR, [SP, #-16]!

    MOV FP, SP

    STP X19, X20, [SP, #-16]!

    STP Q8, Q9, [SP, #-32]!
    STP Q10, Q11, [SP, #-32]!

    MOV X19, X0
    MOV X20, #0

    FMOV S8, S0                // Move the contrast factor into S8 to avoid overwriting it during
                               // the loading of pixel data

    // Load all the constants
    LDR S9, FP_128_S_CONST
    LDR S10, FP_ZERO_S_CONST
    LDR S11, FP_255_S_CONST

    FMOV W21, S8
    DUP V8.4S, W21             // Duplicate contrast factor across all single-precision floating-point words

    FMOV W22, S9
    DUP V9.4S, W22             // Duplicate 128.0 across all single-precision floating-point words

    FMOV W23, S10
    DUP V10.4S, W23            // Duplicate 0.0 across all single-precision floating-point words

    FMOV W24, S11
    DUP V11.4S, W24            // Duplicate 255.0 across all single-precision floating-point words

loop_rows:
    CBZ W2, exit_contrast
    ADD X0, X19, X20

    LSR W4, W1, #4
    CBZ W4, process_remaining_pixels

vectors:
    LD4 { V0.8B, V1.8B, V2.8B, V3.8B }, [X0], #32

    LD4 { V4.8B, V5.8B, V6.8B, V7.8B }, [X0], #32

    // Perform the contrast calculation on the RGB values. Alpha values remain unchanged.
    // First we convert the bytes of the pixels to single-precision floating-point words.
    UXTL V12.8H, V0.8B         // Extend the unsigned byte to half-word
    UXTL V20.4S, V12.4H        // Extend the unsigned lower half-word to float word
    UXTL2 V21.4S, V12.8H       // Extend the unsigned upper half-word to float word
    UCVTF V20.4S, V20.4S       // Convert the extended integers to floats
    UCVTF V21.4S, V21.4S

    UXTL V13.8H, V1.8B
    UXTL V22.4S, V13.4H
    UXTL2 V23.4S, V13.8H
    UCVTF V22.4S, V22.4S
    UCVTF V23.4S, V23.4S

    UXTL V14.8H, V2.8B
    UXTL V24.4S, V14.4H
    UXTL2 V25.4S, V14.8H
    UCVTF V24.4S, V24.4S
    UCVTF V25.4S, V25.4S

    UXTL V12.8H, V4.8B
    UXTL V26.4S, V12.4H
    UXTL2 V27.4S, V12.8H
    UCVTF V26.4S, V26.4S
    UCVTF V27.4S, V27.4S

    UXTL V13.8H, V5.8B
    UXTL V28.4S, V13.4H
    UXTL2 V29.4S, V13.8H
    UCVTF V28.4S, V28.4S
    UCVTF V29.4S, V29.4S

    UXTL V14.8H, V6.8B
    UXTL V30.4S, V14.4H
    UXTL2 V31.4S, V14.8H
    UCVTF V30.4S, V30.4S
    UCVTF V31.4S, V31.4S

    // Subtract 128 from the RGB values
    FSUB V20.4S, V20.4S, V9.4S
    FSUB V21.4S, V21.4S, V9.4S
    FSUB V22.4S, V22.4S, V9.4S
    FSUB V23.4S, V23.4S, V9.4S
    FSUB V24.4S, V24.4S, V9.4S
    FSUB V25.4S, V25.4S, V9.4S
    FSUB V26.4S, V26.4S, V9.4S
    FSUB V27.4S, V27.4S, V9.4S
    FSUB V28.4S, V28.4S, V9.4S
    FSUB V29.4S, V29.4S, V9.4S
    FSUB V30.4S, V30.4S, V9.4S
    FSUB V31.4S, V31.4S, V9.4S

    // Multiply the RGB values by the contrast factor
    FMUL V20.4S, V20.4S, V8.4S
    FMUL V21.4S, V21.4S, V8.4S
    FMUL V22.4S, V22.4S, V8.4S
    FMUL V23.4S, V23.4S, V8.4S
    FMUL V24.4S, V24.4S, V8.4S
    FMUL V25.4S, V25.4S, V8.4S
    FMUL V26.4S, V26.4S, V8.4S
    FMUL V27.4S, V27.4S, V8.4S
    FMUL V28.4S, V28.4S, V8.4S
    FMUL V29.4S, V29.4S, V8.4S
    FMUL V30.4S, V30.4S, V8.4S
    FMUL V31.4S, V31.4S, V8.4S

    // Add 128 to the RGB values
    FADD V20.4S, V20.4S, V9.4S
    FADD V21.4S, V21.4S, V9.4S
    FADD V22.4S, V22.4S, V9.4S
    FADD V23.4S, V23.4S, V9.4S
    FADD V24.4S, V24.4S, V9.4S
    FADD V25.4S, V25.4S, V9.4S
    FADD V26.4S, V26.4S, V9.4S
    FADD V27.4S, V27.4S, V9.4S
    FADD V28.4S, V28.4S, V9.4S
    FADD V29.4S, V29.4S, V9.4S
    FADD V30.4S, V30.4S, V9.4S
    FADD V31.4S, V31.4S, V9.4S

    // Clamp the calculated values to the range of 0 to 255
    FMAX V20.4S, V20.4S, V10.4S
    FMIN V20.4S, V20.4S, V11.4S
    FMAX V21.4S, V21.4S, V10.4S
    FMIN V21.4S, V21.4S, V11.4S
    FMAX V22.4S, V22.4S, V10.4S
    FMIN V22.4S, V22.4S, V11.4S
    FMAX V23.4S, V23.4S, V10.4S
    FMIN V23.4S, V23.4S, V11.4S
    FMAX V24.4S, V24.4S, V10.4S
    FMIN V24.4S, V24.4S, V11.4S
    FMAX V25.4S, V25.4S, V10.4S
    FMIN V25.4S, V25.4S, V11.4S
    FMAX V26.4S, V26.4S, V10.4S
    FMIN V26.4S, V26.4S, V11.4S
    FMAX V27.4S, V27.4S, V10.4S
    FMIN V27.4S, V27.4S, V11.4S
    FMAX V28.4S, V28.4S, V10.4S
    FMIN V28.4S, V28.4S, V11.4S
    FMAX V29.4S, V29.4S, V10.4S
    FMIN V29.4S, V29.4S, V11.4S
    FMAX V30.4S, V30.4S, V10.4S
    FMIN V30.4S, V30.4S, V11.4S
    FMAX V31.4S, V31.4S, V10.4S
    FMIN V31.4S, V31.4S, V11.4S

    // Convert the single-precision floating-point words back to bytes.
    FCVTNS V20.4S, V20.4S      // Convert the floats back into signed integers
    FCVTNS V21.4S, V21.4S
    UQXTN V12.4H, V20.4S       // Narrow down the floating-point words to half-words
    UQXTN V13.4H, V21.4S
    MOV V12.D[1], V13.D[0]     // Move the value in V13 to the upper half of V12
    UQXTN V0.8B, V12.8H        // Narrow down the half-word back to byte

    FCVTNS V22.4S, V22.4S
    FCVTNS V23.4S, V23.4S
    UQXTN V13.4H, V22.4S
    UQXTN V14.4H, V23.4S
    MOV V13.D[1], V14.D[0]
    UQXTN V1.8B, V13.8H

    FCVTNS V24.4S, V24.4S
    FCVTNS V25.4S, V25.4S
    UQXTN V14.4H, V24.4S
    UQXTN V12.4H, V25.4S
    MOV V14.D[1], V12.D[0]
    UQXTN V2.8B, V14.8H

    FCVTNS V26.4S, V26.4S
    FCVTNS V27.4S, V27.4S
    UQXTN V12.4H, V26.4S
    UQXTN V13.4H, V27.4S
    MOV V12.D[1], V13.D[0]
    UQXTN V4.8B, V12.8H

    FCVTNS V28.4S, V28.4S
    FCVTNS V29.4S, V29.4S
    UQXTN V13.4H, V28.4S
    UQXTN V14.4H, V29.4S
    MOV V13.D[1], V14.D[0]
    UQXTN V5.8B, V13.8H

    FCVTNS V30.4S, V30.4S
    FCVTNS V31.4S, V31.4S
    UQXTN V14.4H, V30.4S
    UQXTN V12.4H, V31.4S
    MOV V14.D[1], V12.D[0]
    UQXTN V6.8B, V14.8H

    SUB X9, X0, #64
    ST4 { V0.8B, V1.8B, V2.8B, V3.8B }, [X9]
    ADD X9, X9, #32
    ST4 { V4.8B, V5.8B, V6.8B, V7.8B }, [X9]

    SUBS W4, W4, #1
    B.NE vectors

process_remaining_pixels:
    ANDS W4, W1, #15
    B.EQ end_row_processing

    LSR W5, W4, #3
    CBZ W5, singles_remainder

vectors_remainder:
    LD4 { V0.8B, V1.8B, V2.8B, V3.8B }, [X0], #32

    // Perform contrast calculation
    UXTL V12.8H, V0.8B
    UXTL V20.4S, V12.4H
    UXTL2 V21.4S, V12.8H
    UCVTF V20.4S, V20.4S
    UCVTF V21.4S, V21.4S

    UXTL V13.8H, V1.8B
    UXTL V22.4S, V13.4H
    UXTL2 V23.4S, V13.8H
    UCVTF V22.4S, V22.4S
    UCVTF V23.4S, V23.4S

    UXTL V14.8H, V2.8B
    UXTL V24.4S, V14.4H
    UXTL2 V25.4S, V14.8H
    UCVTF V24.4S, V24.4S
    UCVTF V25.4S, V25.4S

    FSUB V20.4S, V20.4S, V9.4S
    FSUB V21.4S, V21.4S, V9.4S
    FSUB V22.4S, V22.4S, V9.4S
    FSUB V23.4S, V23.4S, V9.4S
    FSUB V24.4S, V24.4S, V9.4S
    FSUB V25.4S, V25.4S, V9.4S

    FMUL V20.4S, V20.4S, V8.4S
    FMUL V21.4S, V21.4S, V8.4S
    FMUL V22.4S, V22.4S, V8.4S
    FMUL V23.4S, V23.4S, V8.4S
    FMUL V24.4S, V24.4S, V8.4S
    FMUL V25.4S, V25.4S, V8.4S

    FADD V20.4S, V20.4S, V9.4S
    FADD V21.4S, V21.4S, V9.4S
    FADD V22.4S, V22.4S, V9.4S
    FADD V23.4S, V23.4S, V9.4S
    FADD V24.4S, V24.4S, V9.4S
    FADD V25.4S, V25.4S, V9.4S

    FMAX V20.4S, V20.4S, V10.4S
    FMIN V20.4S, V20.4S, V11.4S
    FMAX V21.4S, V21.4S, V10.4S
    FMIN V21.4S, V21.4S, V11.4S
    FMAX V22.4S, V22.4S, V10.4S
    FMIN V22.4S, V22.4S, V11.4S
    FMAX V23.4S, V23.4S, V10.4S
    FMIN V23.4S, V23.4S, V11.4S
    FMAX V24.4S, V24.4S, V10.4S
    FMIN V24.4S, V24.4S, V11.4S
    FMAX V25.4S, V25.4S, V10.4S
    FMIN V25.4S, V25.4S, V11.4S

    FCVTNS V20.4S, V20.4S
    FCVTNS V21.4S, V21.4S
    UQXTN V12.4H, V20.4S
    UQXTN V13.4H, V21.4S
    MOV V12.D[1], V13.D[0]
    UQXTN V0.8B, V12.8H

    FCVTNS V22.4S, V22.4S
    FCVTNS V23.4S, V23.4S
    UQXTN V13.4H, V22.4S
    UQXTN V14.4H, V23.4S
    MOV V13.D[1], V14.D[0]
    UQXTN V1.8B, V13.8H

    FCVTNS V24.4S, V24.4S
    FCVTNS V25.4S, V25.4S
    UQXTN V14.4H, V24.4S
    UQXTN V12.4H, V25.4S
    MOV V14.D[1], V12.D[0]
    UQXTN V2.8B, V14.8H

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

    // Perform contrast calculation.
    // Directly convert the RGB values to floats.
    UCVTF S21, W9
    UCVTF S22, W10
    UCVTF S23, W11

    // Calculate the RGB values as before.
    FSUB S21, S21, S9
    FSUB S22, S22, S9
    FSUB S23, S23, S9

    FMUL S21, S21, S8
    FMUL S22, S22, S8
    FMUL S23, S23, S8

    FADD S21, S21, S9
    FADD S22, S22, S9
    FADD S23, S23, S9

    // Clamp the values to the range of 0 to 255.
    FMAX S21, S21, S10
    FMIN S21, S21, S11
    FMAX S22, S22, S10
    FMIN S22, S22, S11
    FMAX S23, S23, S10
    FMIN S23, S23, S11

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

exit_contrast:
    LDP Q10, Q11, [SP], #32
    LDP Q8, Q9, [SP], #32

    LDP X19, X20, [SP], #16

    LDP FP, LR, [SP], #16

    RET
