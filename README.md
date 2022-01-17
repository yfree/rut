# Rut Database Server
## Powerful, reliable, modern, not SQL

![Rut Database](logo_small.jpg)

The full user guide can be found here [Rut Database and Rut Querying Language User Guide](RUT.md).

Why I built Rut Database
---
I believe that databases are the coolest thing there is in IT. So why is it that we are using SQL and its decrepit equivalents instead of something modern and fresh as we do in every other aspect of computing?

What is Rut Database?
---
Rut Database is a full service database server that can be used for anything from small websites to massive enterprise applications. Rut is just as powerful as a SQL Database or an XML implementation, but much simpler - fueled by a unique and elegant querying language, (Rut Querying Language). Rut is free as in free beer, free as in awesome.

Premise
---
The Rut Databse model approaches data similiar to XML, with hierarchical nodes that contain names and values. But just because Rut is hierarchical doesn't mean that Rut doesn't understand relations between records nor does it mean that Rut Database has to be difficult to operate in order to perform powerful tasks. This is not one of those terrible hierarchical DBs from 50 years ago. Rut Database is designed to place user friendliness before all other aspects of the language, and as such, database operations are extremely flexible. It is powered by an elegant querying language that has an easy-to-learn consistent syntax, which is how the magic happens. Rut Database contains all of the sophistication of XML and SQL but none of their pain. Although, Rut acts very similiarly to XML in terms of its structure, Rut does not actually ever use XML to store its data, it has its own construct for that.

Rut Database
---
A database needs to be an environment of productivity that has all the needed features already installed and ready to use. Right out of the box, Rut can start pumping random names, numbers, dates, addresses, even very specific content, generating millions of random records. Solving common tasks is easy, for example, turning data into XML or JSON output is as simple as adding 'as XML' or 'as JSON' to a read statement. A modern database environment is an environment that works for you and has already solved most of the problems concerning software component gathering.

Rut Data Structure
---
Every piece of data is a node in a non-binary tree structure, starting with the top root node. A node may be a database, it may be a row, it may be a composite value, or it may be a nested composite value. This is entirely up to how the database developer designs their system. There are two types of nodes, a branch node and a leaf node. A branch node contains other child nodes while a leaf node does not. Every node may contain one value, but generally only leaf nodes will have values. A set of values forms a record. Record sets can be joined relationally with other record sets during queries. INNER JOIN and LEFT/RIGHT OUTER JOIN will be supported.
No XML libraries (or any non-Java core libraries for that matter) are used in this project so that I do not get locked into any libraries but my own.

Rut Querying Language
---
The querying language takes on a minimalist approach and has as few operations as possible. Some people want their languages to read like English and some people want their languages to read like a math formula. I want my querying language to read like English! The language does not use non-character symbols in any way other than how the English language intended for them to be used. For instance, you won't find strings such as this :? anywhere near my querying language, unless the symbols are being used inside a Regular Expression. I want the language to be simple yet still be powerful like SQL, supporting everything to include arithmetic operations, aggregate functions, subqueries, and joins. There is only a handful of operations for both DDL and DML - and due to the structure of Rut Database, there is no distinction between DDL and DML. You literally 'read' from the database and 'write' to the database. Sometimes you might 'delete' or 'rename' a node. And when you are done using the interactive shell, feel free to 'exit'. Congrats, you have basically just learned Rut! There are a couple nuances to the language though that will be described in detail once the implementation is complete.

## Supported Data Types
- Text
- Integer
- Decimal
- Time
- Date
- Boolean

## Supported Input Formats
- Rut Querying Language
- XML - real XML, not flat SQL tables
- Json - real Json, not flat SQL tables

## Supported Output Formats
- Basic Rut Textual Output
- XML
- Json

## Rut Querying Language Operations
| Operation | Description |
| ----------- | ----------- |
| read | reads from the database |
| write | writes to the database: creates data, updates data, sets data rules |
| delete | removes node(s) |
| rename | renames node(s) |
| begin  | begin a transaction |
| commit | commit a transaction |
| rollback | rollback a transaction |
| undo     | undo a single statement |
| redo     | redo a single statement |
|  //  | comment           |
| exit | exit the interactive shell |

## Rut Querying Language Rules
|Rule Name|Rule Value|
|---------|----------|
|type | Text, Integer, Decimal, Boolean, Date, Time |
|max | Numeric or Decimal, depending on the data type |
|min | Numeric or Decimal, depending on the data type |
|required | true or false |
|unique | true or false |


## Rut Querying Language Keywords
|Keyword|Parameter|Description|
|-------|---------|-----------|
|Root|  |References the top of the Node tree |
|Child | |Substitutes for all of the child nodes of its specified parent
|Integer |A number specifying max allowable length|A random integer
|Decimal |A number specifying max allowable length|A random decimal number
|Boolean | |A random boolean
|Text |A number specifying max allowable length|A random string of text
|Date |'now' returns current date|A random date
|Time |'now' returns current time|A random time
|Newid| | Database wide unique numeric identifier
|FirstNameMale |'unique' returns a database wide unique name|A random male first name from the built in library
|FirstNameFemale |'unique' returns a database wide unique name|A random female first name from the built in library
|LastName |'unique' returns a database wide unique name|A random last name from the built in library

More details about these operations can be found in the [Rut Querying Language User Guide](RUT.md).

Transactions
---
Transactions will work the same way in Rut Database as they do in most modern databases. Commit and rollback are the transaction's main functions. When a transaction begins, the nodes that are used are locked and only writable by the owner of the Shell ID associated with those Nodes. Once the transaction is committed or rolled back, the nodes are unlocked and the changes are visible to the world.

Connection
---
Statements are executed sequentially from the command line, the interactive shell, or through a TCP/IP connection. In order to read from a script file, use redirection as you would do for any other database server. A connection over SSL is most likely going to be enforced. When connecting through a TCP/IP connection, the server will accept statements sequentially and return the results of each query in plain text. Concurrent read and write will be fully supported, with node locks used when needed. Rut Database will be fully ACID compliant, supporting real transactions.

When this project reaches a point of stability, I will begin building the libraries required to easily use this database with all of the popular languages.

Suggestions and Constructive Criticism
---
Feedback is highly encouraged!

License
---
Apache 2.0.

### Feel free to check out my progress!
