  .text
  .globl target
  .type target, @function

#! file-offset 0
#! rip-offset  0
#! capacity    4 bytes

# Text                              #  Line  RIP   Bytes  Opcode              
.target:                            #        0     0      OPC=<label>         
  movq $0x0, %rbx                   #  1     0     10     OPC=movq_r64_imm64  
  callq .move_016_008_cx_r8b_r9b    #  2     0xa   5      OPC=callq_label     
  callq .move_r8b_to_byte_2_of_rbx  #  3     0xf   5      OPC=callq_label     
  callq .move_008_016_r8b_r9b_bx    #  4     0x14  5      OPC=callq_label     
  callq .move_r9b_to_byte_2_of_rbx  #  5     0x19  5      OPC=callq_label     
  retq                              #  6     0x1e  1      OPC=retq            
                                                                              
.size target, .-target