    .global invert
    .type invert, %function
    .p2align 4

// Function signature (parameters passed in registers):
// X0: Pixels pointer (uint8_t* pixels) - Base address of the bitmap pixel data
// W1 (or X1): Width of bitmap (uint32_t width) - Number of pixels per row
// W2 (or X2): Height of bitmap (uint32_t height) - Number of rows
// W3 (or X3): Stride (Bytes per row) (uint32_t stride) - Total bytes to move to the next row

invert:
    STP FP, LR, [SP, #-16]!

    MOV FP, SP

    STP X19, X20, [SP, #-16]!

    MOV X19, X0
    MOV X20, #0

    MOV W6, #0xFF              // Load 0xFF (255) into W6 for inversion constant
    DUP V16.8B, W6             // Duplicate 0xFF across all bytes of V16

loop_rows:
    CBZ W2, exit_invert
    ADD X0, X19, X20

    LSR W4, W1, #4
    CBZ W4, process_remaining_pixels

vectors:
    LD4 { V0.8B, V1.8B, V2.8B, V3.8B }, [X0], #32

    LD4 { V4.8B, V5.8B, V6.8B, V7.8B }, [X0], #32

    // Perform the invert calculation on the RGB values. Alpha values remain unchanged.
    // Calculation involves subtracting RGB values from 255 (0xFF).
    // We use UQSUB to perform unsigned subtraction, since RGBA values are 8-bit unsigned
    // ranging from 0 to 255. UQSUB also saturates the value, essentially clamping it within
    // the 0 to 255 range.
    UQSUB V0.8B, V16.8B, V0.8B
    UQSUB V1.8B, V16.8B, V1.8B
    UQSUB V2.8B, V16.8B, V2.8B

    UQSUB V4.8B, V16.8B, V4.8B
    UQSUB V5.8B, V16.8B, V5.8B
    UQSUB V6.8B, V16.8B, V6.8B

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

    // Perform invert calculation
    UQSUB V0.8B, V16.8B, V0.8B
    UQSUB V1.8B, V16.8B, V1.8B
    UQSUB V2.8B, V16.8B, V2.8B

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

    // Perform invert calculation.
    // We use SUB here since there's no unsigned version of the SUB instruction.
    // We also don't need to clamp the values since the RGBA values will be in the range
    // 0 to 255.
    SUB W9, W6, W9
    SUB W10, W6, W10
    SUB W11, W6, W11

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

exit_invert:
    LDP X19, X20, [SP], #16

    LDP FP, LR, [SP], #16

    RET
