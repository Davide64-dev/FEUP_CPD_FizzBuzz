# Distributed Computing Project

The development of this program is part of the curriculum for [Parallel and Distributed Computing](https://sigarra.up.pt/feup/en/ucurr_geral.ficha_uc_view?pv_ocorrencia_id=520333) course.

## Run Server

```shell
javac -d bin -cp bin:lib/ $(find src -name "*.java") 
```

```shell
java -cp bin game.GameServer <PORT>
```

## Run Client

```shell
javac -d bin -cp bin:lib/ $(find src -name "*.java") 
```

```shell
java -cp bin game.GameClient <IP> <PORT>
```
