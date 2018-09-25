.PHONY=slave host default clean .burn .reset
default:host slave

.hb:
	@make -f light328.mk MODE=HOST .burn

.sb:
	@make -f light328.mk MODE=SLAVE .uart_upload

.reset:
	@make -f light328.mk .reset


slave:
	@echo !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	@echo !!!!!!!!!!!!!!!!!!!build slave image!!!!!!!!!!!!!!
	@echo !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	@make -f light328.mk MODE=SLAVE

host:
	@echo !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	@echo !!!!!!!!!!!!!!!!!!!build host image !!!!!!!!!!!!!!
	@echo !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	@make -f light328.mk MODE=HOST


clean:
	@make -f light328.mk clean