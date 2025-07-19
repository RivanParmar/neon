    .global brightness
    .type brightness, %function
    .p2align 4

// Function signature (parameters passed in registers):
// X0: Pixels pointer (uint8_t* pixels) - Base address of the bitmap pixel data
// W1 (or X1): Width of bitmap (uint32_t width) - Number of pixels per row
// W2 (or X2): Height of bitmap (uint32_t height) - Number of rows
// W3 (or X3): Stride (Bytes per row) (uint32_t stride) - Total bytes to move to the next row
// W4 (or X4): Brightness (int32_t brightness) - Amount of brightness to be applied

brightness:
    STP FP, LR, [SP, #-16]!

    MOV FP, SP

    STP X19, X20, [SP, #-16]!

    STP Q8, Q9, [SP, #-32]!

    MOV X19, X0
    MOV X20, #0

    DUP V8.8H, W4              // Duplicate the brightness value across all half-words of V8

    MOV W21, #0                // These will be used during calculation of single pixels
    MOV W22, #255

loop_rows:
    CBZ W2, exit_brightness
    ADD X0, X19, X20

    LSR W5, W1, #4
    CBZ W5, process_remaining_pixels

vectors:
    LD4 { V0.8B, V1.8B, V2.8B, V3.8B }, [X0], #32

    LD4 { V4.8B, V5.8B, V6.8B, V7.8B }, [X0], #32

    // Perform the brightness calculation on the RGB values. Alpha values remain unchanged.
    // Calculation involves adding the signed value of the brightness parameter to the pixel's RGB values
    // The same process is carried out for each pixel.
    UXTL V0.8H, V0.8B          // Convert the pixels' bytes to half-words
    USQADD V0.8H, V8.8H        // Add the signed value of brightness to the pixels' unsigned value
    UQXTN V0.8B, V0.8H         // Convert the half-words back to bytes

    UXTL V1.8H, V1.8B
    USQADD V1.8H, V8.8H
    UQXTN V1.8B, V1.8H

    UXTL V2.8H, V2.8B
    USQADD V2.8H, V8.8H
    UQXTN V2.8B, V2.8H

    UXTL V4.8H, V4.8B
    USQADD V4.8H, V8.8H
    UQXTN V4.8B, V4.8H

    UXTL V5.8H, V5.8B
    USQADD V5.8H, V8.8H
    UQXTN V5.8B, V5.8H

    UXTL V6.8H, V6.8B
    USQADD V6.8H, V8.8H
    UQXTN V6.8B, V6.8H

    SUB X9, X0, #64
    ST4 { V0.8B, V1.8B, V2.8B, V3.8B }, [X9]
    ADD X9, X9, #32
    ST4 { V4.8B, V5.8B, V6.8B, V7.8B }, [X9]

    SUBS W5, W5, #1
    B.NE vectors

process_remaining_pixels:
    ANDS W5, W1, #15
    B.EQ end_row_processing

    LSR W6, W5, #3
    CBZ W6, singles_remainder

vectors_remainder:
    LD4 { V0.8B, V1.8B, V2.8B, V3.8B }, [X0], #32

    // Perform brightness calculation
    UXTL V0.8H, V0.8B
    USQADD V0.8H, V8.8H
    UQXTN V0.8B, V0.8H

    UXTL V1.8H, V1.8B
    USQADD V1.8H, V8.8H
    UQXTN V1.8B, V1.8H

    UXTL V2.8H, V2.8B
    USQADD V2.8H, V8.8H
    UQXTN V2.8B, V2.8H

    SUB X9, X0, #32
    ST4 { V0.8B, V1.8B, V2.8B, V3.8B }, [X9]

    SUB W5, W5, #8
    CBZ W5, end_row_processing

singles_remainder:
    CBZ W5, end_row_processing

singles:
    LDR W8, [X0]

    AND W9, W8, #0xFF
    LSR W10, W8, #8
    AND W10, W10, #0xFF
    LSR W11, W8, #16
    AND W11, W11, #0xFF
    LSR W12, W8, #24

    // Perform brightness calculation.
    // There's no direct equivalent of USQADD in the base instructions. Therefore, we use the base
    // ADD instruction which does signed addition. This can produce negative values and because of
    // that we need to clamp the RGB values in the range of 0 to 255.
    ADD W9, W9, W4
    ADD W10, W10, W4
    ADD W11, W11, W4

    // Clamp Red (W9) to 0-255
    CMP W9, W21                // Compare with 0
    CSEL W9, W9, W21, GE       // if W9 >= 0, W9 = W9, else W9 = 0
    CMP W9, W22                // Compare with 255
    CSEL W9, W9, W22, LE       // if W9 <= 255, W9 = W9, else W9 = 255

    // Clamp Green (W10) to 0-255
    CMP W10, W21
    CSEL W10, W10, W21, GE
    CMP W10, W22
    CSEL W10, W10, W22, LE

    // Clamp Blue (W11) to 0-255
    CMP W11, W21
    CSEL W11, W11, W21, GE
    CMP W11, W22
    CSEL W11, W11, W22, LE

    LSL W8, W12, #24
    LSL W11, W11, #16
    ORR W8, W8, W11
    LSL W10, W10, #8
    ORR W8, W8, W10
    ORR W8, W8, W9

    STR W8, [X0]

    ADD X0, X0, #4

    SUBS W5, W5, #1
    B.NE singles

end_row_processing:
    ADD X20, X20, X3

    SUBS W2, W2, #1
    B.NE loop_rows

exit_brightness:
    LDP Q8, Q9, [SP], #32

    LDP X19, X20, [SP], #16

    LDP FP, LR, [SP], #16

    RET