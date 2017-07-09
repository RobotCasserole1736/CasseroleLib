
//Note - this PORT string must be aligned with the port the webserver is served on.
var port = "5805";
var hostname = window.location.hostname+":"+port;

var dataSocket = new WebSocket("ws://"+hostname+"/rtplot")

var signal_names = []
var signal_units = []
var signal_display_names = []


var global_chart;

var time_range_sec = 10.0;


dataSocket.onopen = function (event) {
    document.getElementById("id01").innerHTML = "COM Status: Socket Opened.";
    document.getElementById("stop_btn").disabled = true;
    document.getElementById("start_btn").disabled = false;
};

dataSocket.onerror = function (error) {
    document.getElementById("id01").innerHTML = "COM Status: Error with socket. Reconnect to robot, open driver station, then refresh this page.";
    alert("ERROR from RT Plot: Robot Disconnected!!!\n\nAfter connecting to the robot, open the driver station, then refresh this page.");
    document.getElementById("stop_btn").disabled = true;
    document.getElementById("start_btn").disabled = true;
};

dataSocket.onclose = function (error) {
    document.getElementById("id01").innerHTML = "COM Status: Error with socket. Reconnect to robot, open driver station, then refresh this page.";
    alert("ERROR from RT Plot: Robot Disconnected!!!\n\nAfter connecting to the robot, open the driver station, then refresh this page.");
    document.getElementById("stop_btn").disabled = true;
    document.getElementById("start_btn").disabled = true;
};

dataSocket.onmessage = function (event) {
    var data = JSON.parse(event.data);
    if(data.type == "daq_update"){
        addDataToPlot(data.samples);
    } else if(data.type == "signal_list"){
        genSignalListTable(data.signals);
    }

};

//Given a chunk of json data which is presumed to represent a set of 
// samples for the currently-plotted signals, add that data to the plot.
function addDataToPlot(data){
    var sig_iter;
    var samp_iter;
    var samp_time;
    var samp_val;
    var newest_timestamp = 0;
    
    //Iterate over all samples in all signals recieved
    for(sig_iter = 0; sig_iter < data.length; sig_iter++){
        for(samp_iter = 0; samp_iter < data[sig_iter].samples.length; samp_iter++){
            
            //Parse each sample time&value
            samp_time = parseFloat(data[sig_iter].samples[samp_iter].time);
            samp_val = parseFloat(data[sig_iter].samples[samp_iter].val);
            
            //Keep track of the most recent sample of all the data
            if(samp_time > newest_timestamp){
                newest_timestamp = samp_time;
            }
            
            //Add the saple to the plot
            global_chart.series[sig_iter].addPoint([samp_time,samp_val],false,false,true);
        }
    }
    
    global_chart.xAxis[0].setExtremes(newest_timestamp - time_range_sec,newest_timestamp,false)
    //Force a chart update to display the table
    global_chart.redraw();
    
}

function genSignalListTable(arr){
    var i;
    var col_counter = 0;
    var SIGNALS_PER_ROW = 1; //meh. html is hard.
    signal_names = [];
    
    var out = "<table><tbody><tr>";
    
    for(i = 0; i < arr.length; i++){
        signal_names.push(arr[i].name);
        signal_units.push(arr[i].units);
        signal_display_names.push(arr[i].display_name);
        out += "<td><input type=\"checkbox\" name=\""+arr[i].name+"\" />"+arr[i].display_name+" (" + arr[i].units + ") </td>";
       
        if(col_counter >= (SIGNALS_PER_ROW-1)){
            //start a new row
            col_counter = 0;
            out += "</tr><tr>";
        } else {
            col_counter++;
        }
    }
    out +="</tr></tbody></table>";
    document.getElementById("id02").innerHTML = out;

}

function handleStartBtnClick(){
    var cmd = "start:";
    var temp_series = [];
    var units_to_yaxis_index = [];
    var yaxis_index = 0;
    
    //deep-copy the default chart options
	var options = $.extend(true, {}, dflt_options)
    
    //Destroy any existing chart.
	if(global_chart){
        //double check the user didn't click it by mistake.
        if(confirm('This will clear the current recording. Are you sure?')){
            global_chart.destroy();
        } else {
            return; //do nothing
        }
		
	}
	
    
    //Disable signal selection
    document.getElementById("clear_btn").disabled = true;
    document.getElementById("start_btn").disabled = true;
    for(i = 0; i < signal_names.length; i++){
        checkboxes = document.getElementsByName(signal_names[i]);
        for(var j=0, n=checkboxes.length;j<n;j++) {
            checkboxes[j].disabled = true;
        }
    }
    
    //Select only checked signals
    for(i = 0; i < signal_names.length; i++){
        checkboxes = document.getElementsByName(signal_names[i]);
        for(var j=0, n=checkboxes.length;j<n;j++) {
            
            //For all checked boxes...
            if(checkboxes[j].checked == true){
                //Assemble command for sending to server
                cmd += signal_names[i] + ",";
                
                //Handle grouping like-units signals on the same Y axis
                var unit = signal_units[i];
                if(!(unit in units_to_yaxis_index)){
                    units_to_yaxis_index[unit] = yaxis_index;
                    options.yAxis.push({
                                            title:{
                                                text:unit,
                                                style: {
                                                    color: '#DDD',
                                                },
                                            }, 
                                            showEmpty:false,
                                            lineColor: '#777',
                                            tickColor: '#444',
                                            gridLineColor: '#444',
                                            gridLineWidth: 1,
                                            labels: {
                                                style: {
                                                    color: '#DDD',
                                                    fontWeight: 'bold'
                                                },
                                            },
                                       });
                    yaxis_index++;
                }
                
                // set up chart for signals
                temp_series.push({name:signal_display_names[i],
                                  data:[],
                                  visible:true,
                                  visibility_counter:0,
                                  yAxis:units_to_yaxis_index[unit],
                                  states: {
                                      hover: {
                                          enabled: false
                                      },
                                  },
                                  marker: {
                                      enabled: null
                                  },
                                 });

            }
            

    
            
        }
    }
    
    
    //Create the Highcharts chart just before starting DAQ
        
    //Add all data to the chart
    $.each(temp_series, function(itemNo, element) {
        options.series.push(element);
    });
    //Create dat chart
    global_chart = new Highcharts.Chart(options);

    //Request data from robot
    dataSocket.send(cmd); 
    document.getElementById("stop_btn").disabled = false;
}

function handleStopBtnClick(){
    //Request stopping data from robot
    dataSocket.send("stop:"); 
    
    document.getElementById("stop_btn").disabled = true;
    
    //re-enable siagnal selection
    document.getElementById("clear_btn").disabled = false;
    document.getElementById("start_btn").disabled = false;
    for(i = 0; i < signal_names.length; i++){
        checkboxes = document.getElementsByName(signal_names[i]);
        for(var j=0, n=checkboxes.length;j<n;j++) {
            checkboxes[j].disabled = false;
        }
    }
    
    //Reset chart bounds to all data recieved.
    global_chart.xAxis[0].setExtremes(null,null)
}

function handleRefreshSignalsBtnClick(){
    dataSocket.send("get_list:"); 
}

function handleClearBtnClick(){
    var i;
    //Reset all checkboxes to unchecked.
    for(i = 0; i < signal_names.length; i++){
        checkboxes = document.getElementsByName(signal_names[i]);
        for(var j=0, n=checkboxes.length;j<n;j++) {
            checkboxes[j].checked = false;
        }
    }

}

/**************************************************************************************
 ** HIGHCHARTS SUPPORT
 **************************************************************************************/

var dflt_options =  {    

		credits: {
			enabled: false
		},

		chart: {
			zoomType: 'x',
			renderTo: 'container',
			animation: false,
			ignoreHiddenSeries: true,
            resetZoomButton: {
                position: {
                    align: 'left',
                },
                theme: {
                    fill: '#822',
                    stroke: '#999',
                    r: 3,
                    style: {
                        color: '#999'
                    },
                    states: {
                        hover: {
                            fill: '#782828',
                            style: {
                                color: '#ccc'
                            },
                        },
                    },
                },
            },
			panning: true,
			panKey: 'shift',
			showAxes: true,
            backgroundColor: {
                linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
                stops: [
                    [0, 'rgb(0, 0, 0)'], //Yes, both black. Just in case I decide to change back....
                    [1, 'rgb(0, 0, 0)']
                ]
            },
		},
        
		title: { 
            //disable title
			text: null,
		},
        
		xAxis: {
			type: 'linear',
			title: 'Time (sec)',
            lineColor: '#777',
            tickColor: '#444',
            gridLineColor: '#444',
            gridLineWidth: 1,
            labels: {
                style: {
                    color: '#DDD',
                    fontWeight: 'bold'
                },
            },
            title: {
                style: {
                    color: '#D43',
                },
            },
		},
		
    	yAxis: [],
		
		legend: {
			layout: 'vertical',
            align: 'right',
            verticalAlign: 'top',
            borderWidth: 1,
            floating: true,
            itemStyle: {
                font: '9pt Trebuchet MS, Verdana, sans-serif',
                color: '#DDD'
            },
            itemHoverStyle:{
                color: 'gray'
            }  
			
		},
		
		exporting: {
			enabled: false
		},
		
		colors: ['#FF0000', '#0000FF', '#00FF00','#FF00FF', '#00FFFF', '#FFFF00'],
   
		plotOptions: {
			line: {
				marker: {
					radius: 2
				},
				lineWidth: 1,
				threshold: null,
				animation: false,
			}
		},
		tooltip: {
			crosshairs: true,
			hideDelay: 0,
			shared: true,
			backgroundColor: null,
            snap: 30,
			borderWidth: 1,
            borderColor: '#FF0000',
			shadow: true,
			animation: false,
			useHTML: false,
			style: {
                padding: 0,
                color: '#D43',
            }
        },  

		series: []
	}