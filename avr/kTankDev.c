/* 32 Bytes */
struct kTank_uart_cmd{
    UINT8 devId;
    UINT8 ctrlId;
    UINT8 cmd;
    UINT8 rsv[2]
    unsigned char lowest_pwm;	/* lowest pwm value, this value match 0% */
    unsigned char watt;		/* power, watt */
    INT8 temperature;
    UINT8 value[24];
};

#define KTank_UART_CMD_HEAD  0xa5
#define KTank_UART_CMD_TAIL  0x5a


static char uart_cmd_rx_buf[64];
static UINT8 uart_cmd_rx_pos = 0;

/* packet format
 *   0   |  1     |  2     |  3  |4-35    | 36
 * ---   | -----  | -----  | --  |----    | -------
 *   head|devId   | ctrlID | rsv | data   | tail
 */

void uart_cmd_process(char c)
{
    uart_cmd_rx_buf[pos++] = c;
    if(pos >= 37){		/* packet maybe finished */
	
    }
}
