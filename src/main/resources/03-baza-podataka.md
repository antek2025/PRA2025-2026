# 03 — Baza podataka

**Sustav:** PostgreSQL 18 · **Baza:** `pra_projekt` · **Kodiranje:** UTF-8 (zbog hrvatskih znakova č, ć, ž, š, đ)

---

## Model podataka

```
┌───────────────────────────┐
│         korisnik          │
├───────────────────────────┤
│ id            PK          │
│ ime                       │
│ prezime                   │
│ email         UNIQUE      │
│ lozinka_hash              │
│ uloga         ADMIN|PREDAVAC
│ aktivan                   │
│ datum_kreiranja           │
└──────┬──────────────┬─────┘
       │              │
       │ 1            │ 1
       │              │
       │ N            │ N
┌──────┴──────────┐   │      ┌──────────────────────────┐
│ kolegij_predavac│   │      │        kolegij           │
├─────────────────┤   │      ├──────────────────────────┤
│ kolegij_id  PK,FK├──┼─────▶│ id            PK         │
│ korisnik_id PK,FK│  │      │ sifra         UNIQUE     │
└─────────────────┘   │      │ naziv                    │
                      │      │ opis                     │
                      │      │ ects                     │
                      │      │ semestar                 │
                      │      │ aktivan                  │
                      │      └───────────┬──────────────┘
                      │                  │ 1
                      │                  │
                      │                  │ N
                      │      ┌───────────┴──────────────┐
                      │      │        obavijest         │
                      │      ├──────────────────────────┤
                      │      │ id            PK         │
                      │      │ naslov                   │
                      │      │ opis                     │
                      │      │ datum_objave             │
                      │      │ datum_isteka             │
                      └─────▶│ autor_id      FK         │
                             │ kolegij_id    FK CASCADE │
                             │ datum_kreiranja          │
                             └──────────────────────────┘
```

**Veze:**

| Veza | Kardinalnost | Ostvarena kroz |
|------|--------------|----------------|
| Katedra → Kolegij | 1 : N | `kolegij.katedra_id` (smije biti NULL) |
| Kolegij ↔ Predavač | N : M | spojna tablica `kolegij_predavac` |
| Kolegij → Obavijest | 1 : N | `obavijest.kolegij_id` |
| Korisnik → Obavijest | 1 : N | `obavijest.autor_id` |

### `katedra`

| Stupac | Tip | Ograničenja |
|--------|-----|-------------|
| `id` | `bigint` | PK, identity |
| `kratica` | `varchar(10)` | NOT NULL, **UNIQUE** |
| `naziv` | `varchar(100)` | NOT NULL, **UNIQUE** |
| `opis` | `varchar(500)` | dopušta NULL |

Katedra je ustrojstvena jedinica učilišta (npr. *Programsko inženjerstvo*) pod koju spada
skupina kolegija. Uvedena je da se kolegiji i obavijesti mogu filtrirati po području,
a ne samo po semestru.

**Zašto je `kolegij.katedra_id` nullable?** Kolegij se može unijeti prije nego se odluči
kojoj katedri pripada. Prazna vrijednost je legitimno stanje, a ne greška — u sučelju se
prikazuje kao *„nije određena"*.

**Zašto se katedra s kolegijima ne može obrisati?** Isti razlog kao kod korisnika koji je
autor obavijesti: jasna poruka je bolja od tihog odspajanja dvadesetak kolegija koje bi
netko primijetio tek kasnije. Provjera je u `KatedraServis.obrisi()`.

**Zašto i `naziv` ima UNIQUE, a ne samo `kratica`?** Dvije katedre s istim nazivom, a
različitom kraticom, bile bi neupotrebljive u padajućem izborniku.

---

## Tablice

### `korisnik`

| Stupac | Tip | Ograničenja | Napomena |
|--------|-----|-------------|----------|
| `id` | `bigint` | PK, identity | |
| `ime` | `varchar(50)` | NOT NULL | |
| `prezime` | `varchar(50)` | NOT NULL | |
| `email` | `varchar(120)` | NOT NULL, **UNIQUE** | ujedno korisničko ime kod prijave |
| `lozinka_hash` | `varchar(100)` | NOT NULL | BCrypt hash, **nikad obična lozinka** |
| `uloga` | `varchar(20)` | NOT NULL, CHECK | samo `ADMIN` ili `PREDAVAC` |
| `aktivan` | `boolean` | NOT NULL | deaktivirani se ne može prijaviti |
| `datum_kreiranja` | `timestamp` | NOT NULL | |

**Zašto `email UNIQUE`?** Jer je e-mail korisničko ime. Bez ovoga bi dva korisnika mogla
imati istu adresu i prijava ne bi znala koga pustiti. Ograničenje stoji **u bazi**, a ne
samo u kodu — ako aplikacija ikad krene u dvije instance, provjera u kodu bi imala rupu
(dva zahtjeva istovremeno prođu provjeru), a baza ne može pogriješiti.

**Zašto `uloga` kao `varchar` s CHECK, a ne broj?**
U entitetu stoji `@Enumerated(EnumType.STRING)`. Da je `EnumType.ORDINAL` (broj), promjena
redoslijeda vrijednosti u Java enumu tiho bi promijenila značenje svih redaka u bazi.
Ovako u bazi doslovno piše `ADMIN`, što je i čitljivije kad se gleda kroz `psql`.
CHECK ograničenje Hibernate generira sam.

**Zašto `varchar(100)` za hash?** BCrypt uvijek daje 60 znakova. 100 ostavlja prostora
ako se ikad prijeđe na jači algoritam (npr. Argon2, koji je duži).

**Zašto `aktivan`, kad korisnik može biti obrisan?**
Korisnik koji je autor obavijesti **ne smije** se obrisati — obavijest bi ostala bez autora.
Umjesto brisanja se deaktivira: podaci ostaju, ali se ne može prijaviti. Vidi
[04-backend.md](04-backend.md), odjeljak o brisanju.

---

### `kolegij`

| Stupac | Tip | Ograničenja |
|--------|-----|-------------|
| `id` | `bigint` | PK, identity |
| `sifra` | `varchar(20)` | NOT NULL, **UNIQUE** |
| `naziv` | `varchar(120)` | NOT NULL |
| `opis` | `varchar(1000)` | dopušta NULL |
| `ects` | `integer` | NOT NULL |
| `semestar` | `integer` | NOT NULL |
| `katedra_id` | `bigint` | FK → `katedra`, **dopušta NULL** |
| `aktivan` | `boolean` | NOT NULL |

**Zašto `sifra` osim `id`?** `id` je tehnički ključ za bazu; `sifra` (PRA, BAZ, WEB) je ono
što ljudi stvarno koriste. U sučelju se prikazuje šifra jer je kraća od punog naziva i svi
je prepoznaju. Sprema se uvijek velikim slovima (`KolegijServis` je pretvara), pa "pra" i
"PRA" ne mogu postati dva različita kolegija.

**Zašto `opis` smije biti NULL?** Kolegij se može unijeti prije nego se napiše opis.
Prazan tekst (`""`) i "nema opisa" nisu isto — servis prazan unos svjesno pretvara u `NULL`.

**Zašto nema stupca `godina_studija`?**
Godina se **računa iz semestra** (`Kolegij.godinaStudija()`): semestri 1 i 2 su 1. godina,
3 i 4 su 2., a 5 i 6 su 3. godina. Da su oba podatka u bazi, prije ili kasnije bi netko
spremio „semestar 5, godina 1" i podaci bi si proturječili. Ovako je semestar jedini izvor
istine, a godina se šalje frontendu izračunata u `KolegijDto.godinaStudija`.

Filtriranje po godini zato ne traži novi stupac — 2. godina je jednostavno
`semestar IN (3, 4)`.

**Zašto raspon ECTS-a i semestra nije CHECK u bazi?** Provjere (`1–30` za ECTS, `1–6` za
semestar) su u DTO-u kroz `@Min`/`@Max`. Razlog je poruka o grešci: iz baze bi došla
nerazumljiva greška o prekršenom ograničenju, a ovako korisnik dobije *"Semestar mora biti
između 1 i 6."* na hrvatskom, pored konkretnog polja. Ovo je svjestan kompromis — pravilo
koje je isključivo u aplikaciji.

---

### `kolegij_predavac` (spojna tablica)

| Stupac | Tip | Ograničenja |
|--------|-----|-------------|
| `kolegij_id` | `bigint` | PK (dio), FK → `kolegij` |
| `korisnik_id` | `bigint` | PK (dio), FK → `korisnik` |

**Zašto spojna tablica, a ne stupac `predavac_id` u kolegiju?**
Jer je veza N:M — jedan kolegij može imati više predavača (npr. PRA ima dvoje), a jedan
predavač predaje na više kolegija. Jedan stupac bi ovo mogao samo za 1:N.

**Zašto složeni primarni ključ `(kolegij_id, korisnik_id)`?** Time baza sama sprječava
da isti predavač bude dvaput upisan na isti kolegij. Nije potreban zaseban `id` — par
stupaca je već jedinstven i to je cijeli sadržaj retka.

---

### `obavijest`

| Stupac | Tip | Ograničenja |
|--------|-----|-------------|
| `id` | `bigint` | PK, identity |
| `naslov` | `varchar(150)` | NOT NULL |
| `opis` | `varchar(2000)` | NOT NULL |
| `datum_objave` | `date` | NOT NULL |
| `datum_isteka` | `date` | NOT NULL |
| `kolegij_id` | `bigint` | NOT NULL, FK → `kolegij` **ON DELETE CASCADE** |
| `autor_id` | `bigint` | NOT NULL, FK → `korisnik` (bez kaskade) |
| `datum_kreiranja` | `timestamp` | NOT NULL |

Struktura je izravno iz zahtjeva: *"Obavijest je vezana samo uz jedan kolegij, a osim toga
ima i naziv, opis, datum objave i isteka te je vezana uz kreatora."*

**Zašto `date`, a ne `timestamp` za objavu i istek?**
Zahtjev traži *datum*. Vrijeme bi otvorilo pitanja koja nitko nije postavio (u koliko sati
istječe? koja vremenska zona?). Za `<input type="date">` u HTML-u je `date` točno pravi tip.

**Zašto se "je li obavijest aktivna" ne sprema u bazu?**
Jer ovisi o **današnjem datumu** — vrijednost bi bila netočna već sutradan. Računa se u
letu u `Obavijest.jeAktivna(danas)` i šalje u `ObavijestDto.aktivna`.

**Zašto `ON DELETE CASCADE` na kolegiju, ali NE na autoru?**

| | Ponašanje | Razlog |
|---|-----------|--------|
| Brisanje kolegija | briše i njegove obavijesti | Obavijest bez kolegija nema smisla — vezana je isključivo uz njega. |
| Brisanje autora | **zabranjeno** ako ima obavijesti | Obavijest ima smisla i nakon što predavač ode s učilišta; gubitak povijesti bi bio šteta. Umjesto brisanja se korisnik deaktivira. |

Kaskada je izvedena Hibernate anotacijom `@OnDelete(action = OnDeleteAction.CASCADE)` na
polju `Obavijest.kolegij`, iz koje Hibernate generira strani ključ s `ON DELETE CASCADE`.
**Radi je baza, ne aplikacija** — pa vrijedi i ako netko obriše redak izravno kroz SQL.

Zabrana brisanja autora provjerava se u `KorisnikServis.obrisi()` prije nego se uopće
dođe do baze, jer je tako moguće vratiti razumljivu poruku (*"Korisnik je autor 3 obavijesti
pa se ne može obrisati…"*) umjesto greške o stranom ključu.

---

## Kako nastaje shema

U `application.properties`:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Hibernate pri pokretanju usporedi JPA entitete sa stanjem baze i sam kreira tablice koje
nedostaju, odnosno doda stupce koji nedostaju.

**Zašto `update`, a ne `validate` + Flyway migracije?**

Za projektni zadatak `update` znatno ubrzava razvoj — doda se polje u entitet i to je to,
nema pisanja migracijske skripte. Cijena je to što `update` **nikad ne briše** stupce i
ne mijenja tipove, pa se s vremenom u bazi nakupi smeća.

Za pravu produkciju ovo bi bila prva stvar koju bi trebalo promijeniti: `ddl-auto=validate`
uz Flyway migracije, jer je tada svaka promjena sheme zapisana, pregledana i ponovljiva.
Ovdje je to svjesno izostavljeno kao izvan opsega zadatka.

**Napomena:** ako se u entitetima nešto **preimenuje ili obriše**, najlakše je bazu
napraviti iznova:

```bash
psql -U postgres -c "DROP DATABASE pra_projekt;" -c "CREATE DATABASE pra_projekt WITH ENCODING 'UTF8';"
```

---

## Početni podaci

`konfiguracija/PocetniPodaci.java` puni bazu **samo ako je tablica `korisnik` prazna**.

Zašto ta provjera, a ne `data.sql`: Spring bi `data.sql` pokretao pri svakom podizanju i
rušio se na duplikatima. Ovako se ponovno pokretanje aplikacije ne može dogoditi da
prebriše ili udvostruči podatke koje je netko u međuvremenu unio.

Ubacuje se:

- 1 administrator (e-mail i lozinka iz `application.properties`),
- 9 predavača (lozinka `predavac123`),
- 5 katedri,
- 19 kolegija raspoređenih kroz 3 godine studija i 5 katedri; među njima namjerno i
  dva **neaktivna** te jedan **bez katedre i bez predavača**,
- 20 obavijesti — dio **trenutno vrijedi**, dio je **budući**, a dio **istekao**.

Podaci su namjerno ovako razvedeni: na pet kolegija se ne bi vidjelo ima li filtriranje
po godini i katedri uopće smisla.

Punjenje se može isključiti:

```properties
infoeduka.ucitaj-pocetne-podatke=false
```

---

## Korisni SQL upiti za provjeru

```sql
-- Tko na čemu predaje
SELECT k.sifra, k.naziv, kor.ime || ' ' || kor.prezime AS predavac
FROM kolegij k
LEFT JOIN kolegij_predavac kp ON kp.kolegij_id = k.id
LEFT JOIN korisnik kor        ON kor.id = kp.korisnik_id
ORDER BY k.sifra;
```

```sql
-- Obavijesti koje danas vrijede
SELECT o.naslov, k.sifra, a.ime || ' ' || a.prezime AS autor, o.datum_isteka
FROM obavijest o
JOIN kolegij  k ON k.id = o.kolegij_id
JOIN korisnik a ON a.id = o.autor_id
WHERE CURRENT_DATE BETWEEN o.datum_objave AND o.datum_isteka
ORDER BY o.datum_objave DESC;
```
