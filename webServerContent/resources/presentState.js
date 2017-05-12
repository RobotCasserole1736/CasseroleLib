
var hostname = "roboRIO-1736-FRC.local:5805" //Robot hostname
//var hostname = "localhost:5805" //Local Hostname

var dataSocket = new WebSocket("ws://"+hostname+"/statestream")
var numTransmissions = 0;

dataSocket.onopen = function (event) {
  document.getElementById("id01").innerHTML = "Socket Open";
};

dataSocket.onmessage = function (event) {
  genTable(event.data);
  numTransmissions = numTransmissions + 1;
  document.getElementById("id01").innerHTML = "COM Status: Socket Open. RX Count:" + numTransmissions; 
};

dataSocket.onerror = function (error) {
  document.getElementById("id01").innerHTML = "COM Status: Error with socket. Reconnect to robot, open driver station, then refresh this page.";
  alert("ERROR from Present State: Robot Disconnected!!!\n\nAfter connecting to the robot, open the driver station, then refresh this page.");
};

dataSocket.onclose = function (error) {
  document.getElementById("id01").innerHTML = "COM Status: Error with socket. Reconnect to robot, open driver station, then refresh this page.";
  alert("ERROR from Present State: Robot Disconnected!!!\n\nAfter connecting to the robot, open the driver station, then refresh this page.");
};

function genTable(json_data) {
    var arr = JSON.parse(json_data);
    var i;
    var out = "<table border=\"1\">";

    for(i = 0; i < arr.state_array.length; i++) {
        out += "<tr><td>" +
        arr.state_array[i].name +
        "</td><td style=\"width: 200px;\">" +
        arr.state_array[i].value +
        "</td></tr>";
    }
    out += "</table>";
    document.getElementById("id02").innerHTML = out;
}