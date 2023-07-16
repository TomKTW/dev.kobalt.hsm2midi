var i$pageObjectUid$ = 0;
var txt$pageObjectUid$ = "$pageObjectText$";
var speed$pageObjectUid$ = $pageObjectSpeed$;
document.getElementById("typewriter$pageObjectUid$").innerHTML = "";

function typeWriter$pageObjectUid$() {
    if (i$pageObjectUid$ < txt$pageObjectUid$.length) {
        document.getElementById("typewriter$pageObjectUid$").innerHTML += txt$pageObjectUid$.charAt(i$pageObjectUid$);
        i$pageObjectUid$++;
        setTimeout(typeWriter$pageObjectUid$, speed$pageObjectUid$);
    } else {
        reverseTypeWriter$pageObjectUid$();
    }
};

function reverseTypeWriter$pageObjectUid$() {
    if (i$pageObjectUid$ > 0) {
        i$pageObjectUid$--;
        var value = document.getElementById("typewriter$pageObjectUid$").innerHTML;
        document.getElementById("typewriter$pageObjectUid$").innerHTML = value.slice(0, -1);
        setTimeout(reverseTypeWriter$pageObjectUid$, speed$pageObjectUid$);
    } else {
        typeWriter$pageObjectUid$();
    }
};

typeWriter$pageObjectUid$();