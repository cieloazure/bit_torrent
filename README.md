Bit Torrent
Peer to peer file sharing system to be implemnted for the computer networks course.

1. Prerequisite
    - Java SDK
    My Installation:
    ```
    java -version

    java version "10.0.2" 2018-07-17
    Java(TM) SE Runtime Environment 18.3 (build 10.0.2+13)
    Java HotSpot(TM) 64-Bit Server VM 18.3 (build 10.0.2+13, mixed mode)
    ```
2. Setup Instructions

    1. Without IntelliJ
    - The java files are in src/
    - Compile the code using `javac Peer.java`
    - It will generate `*.class` files
    - `cp -r config/ src/`
    - `cp -r alice.txt src/`
    - `cd out/production/bit_torrent/`


    2. With IntelliJ
    - Compile the code using the Intellij IDE
    - `cp -r config/ out/production/bit_torrent`
    - `cp -r alice.txt out/production/bit_torrent`
    - `cd out/production/bit_torrent/`

3. Execution instructions

    - Currently expecting a `config/` folder with `Config.cfg` `PeerInfo.cfg` files in the root path of tha class files
    - Also expecting a file `alice.txt` in the root path of the class files
    - After setting up necessary config files and alice.txt we can execute our program using
        `java Peer <Peer ID>`
    - Currently expecting Peer ID to be one of the peer ids mentioned in the configuration(PeerInfo.cfg)


4. Additional reading resources

    - [State design pattern](https://en.wikipedia.org/wiki/State_pattern#Java)
    - [Builder design pattern](https://en.wikipedia.org/wiki/Builder_pattern#Java)
    - [Java Concurrent package](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/package-summary.html)
    - [Java Object docs](https://docs.oracle.com/javase/7/docs/api/java/lang/Object.html#notify())

5. Remaining Tasks

    - State machine: Other states to complete the bittorrent protocol
    - Logger: Add logging to a file and make it configurable to display on standard output. Also, add a log per peer per connection on how the state machine is behaving
    - Refactoring: Organize the states in a single package and introduce new packages as needed
    - Issues: BitSet without any data is giving problems in serialization

Contributors:
Akash Shingte
Sharmilee Desai
Tushar Kaley
