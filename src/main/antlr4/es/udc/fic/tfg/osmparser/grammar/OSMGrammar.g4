/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

grammar OSMGrammar;

parse
    : (connectStatement | statement)+
    ;

statement:
    SELECT_SYMBOl elements (
        selectStatement
    )
;

selectStatement:
    OBRA_SYMBOL attributeDefinition (COMMA_SYMBOL? attributeDefinition)* CBRA_SYMBOL
    fromStatement entityStatement bboxStatement
;

fromStatement:
FROM_SYMBOL OPAR_SYMBOL valueExpression CPAR_SYMBOL
;

entityStatement:
TO_SYMBOL entity
;

bboxStatement:
WHERE_SYMBOL bboxDefinition
;

connectStatement:
    CONNECT_SYMBOL TO_SYMBOL dbaseElement OF_SYMBOL typeElement FROM_SYMBOL portElement OF_SYMBOL hostElement WITH_SYMBOL userElement
    AND passwordElement
;

passwordIdentifier
: identifier
| PASSWORD_SYMBOL
;

dbaseElement:
DBASE_SYMBOL EQUAL_SYMBOL identifier
;

typeElement:
TYPE_SYMBOL EQUAL_SYMBOL identifier
;

portElement:
PORT_SYMBOL EQUAL_SYMBOL INT_NUMBER
;

hostElement:
HOST_SYMBOL EQUAL_SYMBOL identifier
| HOST_SYMBOL EQUAL_SYMBOL IP_SYMBOL
;

userElement:
USER_SYMBOL EQUAL_SYMBOL identifier
;

passwordElement:
PASSWORD_SYMBOL EQUAL_SYMBOL passwordIdentifier
;

elements: elementsValues (COMMA_SYMBOL? elementsValues)*;

elementsValues
: NODE
| WAY
| RELATION
;

entity: IDENTIFIER;

bboxDefinition:
    BBOX_SYMBOL EQUAL_SYMBOL OPAR_SYMBOL FLOAT_NUMBER (COMMA_SYMBOL? FLOAT_NUMBER)* CPAR_SYMBOL
;

valueExpression
: OPAR_SYMBOL valueDefinition (separator valueDefinition)* CPAR_SYMBOL (separator valueExpression)*
| valueDefinition (separator valueDefinition)*
;

valueDefinition
: identifier EQUAL_SYMBOL identifier
| identifier
| identifier IS_NOT_NULL_SYMBOL
;

separator
: OR
| AND
;

attribute: identifier (COMMA_SYMBOL? identifier)*;

attributeDefinition:
    attribute ARROW_SYMBOL IDENTIFIER
;

identifier: IDENTIFIER | function | STRING_DPOINTS | IDENTIFIER (MINUS_SYMBOL | UNDER_MINUS_SYMBOL) IDENTIFIER;

function:
    functionName OPAR_SYMBOL arguments? CPAR_SYMBOL // número ilimitado de parámetros
;

functionName: IDENTIFIER;

arguments
: expression (COMMA_SYMBOL? expression)*
;

expression
: function
| bool
| identifier
;

//-----------------------------LEXER RULES----------------------------------------

fragment A : [aA];
fragment B : [bB];
fragment C : [cC];
fragment D : [dD];
fragment E : [eE];
fragment F : [fF];
fragment G : [gG];
fragment H : [hH];
fragment I : [iI];
fragment J : [jJ];
fragment K : [kK];
fragment L : [lL];
fragment M : [mM];
fragment N : [nN];
fragment O : [oO];
fragment P : [pP];
fragment Q : [qQ];
fragment R : [rR];
fragment S : [sS];
fragment T : [tT];
fragment U : [uU];
fragment V : [vV];
fragment W : [wW];
fragment X : [xX];
fragment Y : [yY];
fragment Z : [zZ];

fragment DIGIT    : [0-9];
fragment DIGITS   : DIGIT+;
fragment HEXDIGIT : [0-9a-fA-F];

fragment LETTER_WHEN_UNQUOTED_NO_DIGIT: [a-zA-Z_$\u0080-\uffff];
fragment LETTER_WHEN_UNQUOTED: DIGIT | LETTER_WHEN_UNQUOTED_NO_DIGIT;
// Any letter but without e/E and digits (which are used to match a decimal number).
fragment LETTER_WITHOUT_FLOAT_PART: [a-df-zA-DF-Z_$\u0080-\uffff];

fragment UNDERLINE_SYMBOL : '_';
fragment QUOTE_SYMBOL     : '"';

FROM_SYMBOL     : F R O M;
WHERE_SYMBOL    : W H E R E;
ENTITY_SYMBOL   : E N T I T Y;
SELECT_SYMBOl   : S E L E C T;
BBOX_SYMBOL     : B B O X;
TO_SYMBOL       : T O;
CONNECT_SYMBOL  : C O N N E C T;
OF_SYMBOL       : O F;
DBASE_SYMBOL    : D B A S E;
PORT_SYMBOL     : P O R T D B;
USER_SYMBOL     : U S E R D B;
HOST_SYMBOL     : H O S T D B;
PASSWORD_SYMBOL : P A S S W O R D D B;
WITH_SYMBOL     : W I T H;
TYPE_SYMBOL     : T Y P E D B;

TYPE
    : B O O L E A N
    | L O C A L D A T E
    | S T R I N G
    | I N T E G E R
    | L O N G
    | D O U B L E
    | L I N E S T R I N G
    | M U L T I L I N E S T R I N G
    | P O L Y G O N
    | M U L T I P O L Y G O N
    | P O I N T
    | M U L T I P O I N T
;

bool
: TRUE
| FALSE
;

OBRA_SYMBOL        : '{';
CBRA_SYMBOL        : '}';
OPAR_SYMBOL        : '(';
CPAR_SYMBOL        : ')';
COMMA_SYMBOL       : ',';
PCOMMA_SYMBOL      : ';';
DOT_SYMBOL         : '.';
ARROW_SYMBOL       : '=>';
HTAG_SYMBOL        : '#';
EQUAL_SYMBOL       : '=';
MINUS_SYMBOL       : '-';
UNDER_MINUS_SYMBOL : '_';
DDOTS_SYMBOL       : ':';
IS_NOT_NULL_SYMBOL : 'is not null';
IP_SYMBOL
: DIGITS DOT_SYMBOL DIGITS DOT_SYMBOL DIGITS DOT_SYMBOL DIGITS
;

AND : 'AND';
OR  : 'OR';
NOT : 'NOT';

TRUE  : 'true';
FALSE : 'false';

NODE     : 'node';
WAY      : 'way';
RELATION : 'relation';

INT_NUMBER : MINUS_SYMBOL? DIGITS;
FLOAT_NUMBER : MINUS_SYMBOL? (DIGITS? DOT_SYMBOL)? DIGITS;

WHITESPACE: [ \t\f\r\n] -> channel(HIDDEN); // ignore whitespace
COMMENT: '//' ~[\r\n]* -> skip;
SQL_COMMENT: '--' ~[\r\n]* -> skip;

IDENTIFIER:
    STRING
    | DIGITS+ [eE] (LETTER_WHEN_UNQUOTED_NO_DIGIT LETTER_WHEN_UNQUOTED*)? // Have to exclude float pattern, as this rule matches more.
    | DIGITS+ LETTER_WITHOUT_FLOAT_PART LETTER_WHEN_UNQUOTED*
    | LETTER_WHEN_UNQUOTED_NO_DIGIT LETTER_WHEN_UNQUOTED* // INT_NUMBER matches first if there are only digits.
;

STRING
: QUOTE_SYMBOL ( '\\' [\\"] | ~[\\"\r\n] )* QUOTE_SYMBOL
;

STRING_DPOINTS
: IDENTIFIER DDOTS_SYMBOL IDENTIFIER
;