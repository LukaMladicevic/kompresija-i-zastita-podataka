# Kompresija i zastita podataka

Projekti iz predmeta Kompresija i zastita podataka implementirani u Javi.

## Struktura

```
projekat1/    entropija i kompresija
projekat2/    LDPC kod
```

Uz svaki projekat ide izvestaj.txt sa rezultatima i objasnjenjima.

## Projekat 1

Racunanje bajt-entropije fajla i nekoliko osnovnih metoda kompresije. Implementirani su Shannon-Fano kod, Huffmanov kod, LZ77 i LZW. Za svaki metod postoji i kodiranje i dekodiranje, pa se dekodirani fajl moze uporediti sa originalom. Kodirani podaci se pisu bit po bit, tako da nema trosenja prostora na poravnanje.

Kao test fajl koristi se sekspir.txt, oko 5 MB obicnog teksta. Program ispisuje entropiju, velicinu kodiranog fajla i stepen kompresije za svaki metod.

## Projekat 2

Generisanje LDPC koda i dva nacina dekodiranja. Matrica provere parnosti se gradi Gallagerovim postupkom sa zadatim parametrima, iz nje se izvodi tabela sindroma i korektora i odredjuje kodno rastojanje. Pored toga je implementiran i Gallager B algoritam, pa se vidi na kojoj tezini greske svaki od ta dva pristupa prestaje da radi.

Program pri svakom pokretanju ispisuje i nekoliko provera, da se vidi da su rezultati konzistentni.

## Pokretanje

Potrebna je samo Java za pokretanje.

```bash
javac -d out projekat1/src/kompresija/*.java
cd projekat1 && java -cp ../out kompresija.Main
```

Projekat 2:

```bash
javac -d out projekat2/src/ldpc/*.java
java -cp out ldpc.Main
```
