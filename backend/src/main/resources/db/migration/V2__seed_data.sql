-- Sample content so the app is usable on first start. Edit or delete it from the admin mode.

insert into course (id, language, level, description, position) values
    (1, 'Spanish', 'B2', 'Upper-intermediate Spanish grammar.', 1),
    (2, 'Catalan', 'A1', 'Beginner Catalan.', 2);

insert into category (id, course_id, name, description, position) values
    (1, 1, 'Verbs: Past', 'Past tenses of the indicative.', 1),
    (2, 1, 'Verbs: Subjunctive', 'Forms and uses of the subjunctive mood.', 2),
    (3, 1, 'Connectors', 'Words that link ideas and clauses.', 3),
    (4, 2, 'Verbs: Present', 'The present tense.', 1),
    (5, 2, 'Articles', 'Definite and indefinite articles.', 2);

insert into subject (id, category_id, title, description, position) values
    (1, 1, 'Pretérito imperfecto', 'Habits, descriptions and background in the past.', 1),
    (2, 2, 'Imperfecto de subjuntivo', 'Past subjunctive: forms, triggers and conditional sentences.', 2),
    (3, 3, 'Conectores concesivos', 'Expressing contrast: aunque, a pesar de que, por mucho que…', 3),
    (4, 5, 'Articles definits', 'el, la, l'', els, les', 1),
    (5, 4, 'Present d''indicatiu: verbs en -ar', 'Regular first-conjugation verbs.', 2);

insert into lesson (subject_id, content) values
(1, $md$# Pretérito imperfecto

The *imperfecto* describes the past **as it was going on**, without focusing on its beginning or end.

## Forms

| | -ar (hablar) | -er (comer) | -ir (vivir) |
|---|---|---|---|
| yo | habl**aba** | com**ía** | viv**ía** |
| tú | habl**abas** | com**ías** | viv**ías** |
| él / ella / usted | habl**aba** | com**ía** | viv**ía** |
| nosotros | habl**ábamos** | com**íamos** | viv**íamos** |
| vosotros | habl**abais** | com**íais** | viv**íais** |
| ellos / ustedes | habl**aban** | com**ían** | viv**ían** |

Only three irregular verbs:

- **ser**: era, eras, era, éramos, erais, eran
- **ir**: iba, ibas, iba, íbamos, ibais, iban
- **ver**: veía, veías, veía, veíamos, veíais, veían

## Uses

1. **Habits and repeated actions** — *De niño, **jugaba** al fútbol todos los días.*
2. **Descriptions** of people, places and situations — *La casa **era** grande y **tenía** un jardín.*
3. **Time and age** — ***Eran** las diez. **Tenía** veinte años.*
4. **Background** to another action (in *indefinido*) — ***Llovía** cuando salí de casa.*
5. **Simultaneous actions** — *Mientras yo **cocinaba**, ella **leía**.*
6. **Politeness** — ***Quería** preguntarle una cosa.*

> Tip: if you could say *"used to"* or *"was …-ing"* in English, the imperfecto is usually right.
$md$),
(2, $md$# Imperfecto de subjuntivo

## Forms

Take the **ellos** form of the *pretérito indefinido*, remove **-ron** and add the endings. This works for irregular verbs too.

*hablaron → habla- · tuvieron → tuvie- · fueron → fue-*

| | -ra | -se |
|---|---|---|
| yo | habla**ra** | habla**se** |
| tú | habla**ras** | habla**ses** |
| él / ella / usted | habla**ra** | habla**se** |
| nosotros | hablá**ramos** | hablá**semos** |
| vosotros | habla**rais** | habla**seis** |
| ellos / ustedes | habla**ran** | habla**sen** |

Both endings are correct; **-ra** is more common in speech.

## Uses

1. **Subjunctive triggers in the past** — *Quería que **vinieras**. Me pidió que lo **ayudara**.*
2. **Unlikely or impossible conditions** — *Si **tuviera** tiempo, viajaría más.*
3. **Como si** (as if) — *Habla como si lo **supiera** todo.*
4. **Ojalá** for unlikely wishes — *¡Ojalá **estuvieras** aquí!*
5. **Politeness** with *querer* — ***Quisiera** un café, por favor.*

> Sequence of tenses: main verb in the past or conditional → subordinate verb in imperfecto de subjuntivo.
$md$),
(3, $md$# Conectores concesivos

Concessive connectors introduce an obstacle that **does not stop** the main action.

## Aunque

- **+ indicative**: the speaker presents the obstacle as a real, new fact.
  *Aunque **está** cansado, va a salir.* (He is tired — I'm telling you.)
- **+ subjunctive**: the obstacle is hypothetical, or known but considered irrelevant.
  *Aunque **llueva**, iremos a la playa.* (Even if it rains…)

## Other connectors

| Connector | Mood | Example |
|---|---|---|
| a pesar de que | indicative / subjunctive | *A pesar de que estudió, suspendió.* |
| a pesar de + infinitive / noun | — | *A pesar de la lluvia, salimos.* |
| por mucho que / por más que | subjunctive (usually) | *Por mucho que insistas, no cambiaré de opinión.* |
| si bien | indicative | *Si bien es caro, merece la pena.* |
| pese a (que) | indicative / subjunctive | *Pese a que lo sabía, no dijo nada.* |

## Contrast between sentences

*Sin embargo*, *no obstante* and *aun así* start a new sentence or clause:
*Estaba muy cansado. **Aun así**, terminó el informe.*
$md$),
(4, $md$# Articles definits

| | singular | plural |
|---|---|---|
| masculine | **el** / **l'** | **els** |
| feminine | **la** / **l'** | **les** |

## Apostrophe (l')

- Masculine: **always** before a vowel or *h* — *l'home, l'any, l'hivern*
- Feminine: before a vowel or *h* — *l'aigua, l'escola, l'hora*
  …except before an **unstressed i / u** (with or without *h*): *la universitat, la idea, la història*

The plural never takes an apostrophe: *els homes, les escoles*.

## Contractions with prepositions

| | el | els |
|---|---|---|
| a | **al** | **als** |
| de | **del** | **dels** |
| per | **pel** | **pels** |

*Vaig **al** mercat. El llibre **del** professor.*
$md$),
(5, $md$# Present d'indicatiu: verbs en -ar

Remove **-ar** and add the endings.

| | parlar (to speak) |
|---|---|
| jo | parl**o** |
| tu | parl**es** |
| ell / ella / vostè | parl**a** |
| nosaltres | parl**em** |
| vosaltres | parl**eu** |
| ells / elles / vostès | parl**en** |

Subject pronouns are usually dropped because the ending already shows the person:
*Parlo català. — I speak Catalan.*

## Examples

- *Treballo en una oficina.* — I work in an office.
- *Estudiem anglès els dilluns.* — We study English on Mondays.
- *Escoltes música?* — Do you listen to music?
$md$);

insert into vocabulary (subject_id, term, translation, notes, position) values
    (1, 'soler + infinitivo', 'to usually do something', 'Solía levantarme temprano.', 1),
    (1, 'de niño / de niña', 'as a child', null, 2),
    (1, 'a menudo', 'often', null, 3),
    (1, 'de vez en cuando', 'from time to time', null, 4),
    (1, 'mientras', 'while', null, 5),
    (1, 'en aquella época', 'back then; at that time', null, 6),
    (1, 'antes', 'before; in the past', 'Antes vivía en Madrid.', 7),
    (1, 'cada verano', 'every summer', null, 8),
    (2, 'ojalá', 'I wish; hopefully', '¡Ojalá pudiera ir!', 1),
    (2, 'como si', 'as if', 'Always followed by imperfecto/pluscuamperfecto de subjuntivo.', 2),
    (2, 'quisiera', 'I would like (polite)', null, 3),
    (2, 'si tuviera', 'if I had', null, 4),
    (2, 'pedir que', 'to ask (someone) to', 'Me pidió que lo llamara.', 5),
    (2, 'aconsejar que', 'to advise to', null, 6),
    (2, 'exigir que', 'to demand that', null, 7),
    (2, 'era necesario que', 'it was necessary that', null, 8),
    (3, 'aunque', 'although; even if', 'Indicative = fact; subjunctive = hypothesis.', 1),
    (3, 'a pesar de (que)', 'despite; in spite of', null, 2),
    (3, 'por mucho que', 'however much; no matter how much', 'Usually + subjunctive.', 3),
    (3, 'por más que', 'no matter how much', null, 4),
    (3, 'si bien', 'although; while', 'Formal; + indicative.', 5),
    (3, 'pese a (que)', 'despite', null, 6),
    (3, 'aun así', 'even so', null, 7),
    (3, 'no obstante', 'nevertheless', null, 8),
    (3, 'sin embargo', 'however', null, 9),
    (4, 'l''home', 'the man', null, 1),
    (4, 'la dona', 'the woman', null, 2),
    (4, 'el gos', 'the dog', null, 3),
    (4, 'la casa', 'the house', null, 4),
    (4, 'els nens', 'the children; the boys', null, 5),
    (4, 'les noies', 'the girls', null, 6),
    (4, 'l''aigua', 'the water', null, 7),
    (4, 'l''escola', 'the school', null, 8),
    (4, 'la universitat', 'the university', 'No apostrophe: unstressed u.', 9),
    (5, 'parlar', 'to speak', null, 1),
    (5, 'treballar', 'to work', null, 2),
    (5, 'estudiar', 'to study', null, 3),
    (5, 'escoltar', 'to listen (to)', null, 4),
    (5, 'mirar', 'to look at; to watch', null, 5),
    (5, 'menjar', 'to eat', null, 6),
    (5, 'comprar', 'to buy', null, 7),
    (5, 'ballar', 'to dance', null, 8),
    (5, 'viatjar', 'to travel', null, 9);

-- Move the identity sequences past the explicit ids used above.
select setval(pg_get_serial_sequence('course', 'id'), (select max(id) from course));
select setval(pg_get_serial_sequence('category', 'id'), (select max(id) from category));
select setval(pg_get_serial_sequence('subject', 'id'), (select max(id) from subject));
