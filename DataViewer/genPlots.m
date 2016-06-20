clear;
clc;

plotXSig = 'TIME';

% Defines the plots to be created. Each set of plots represents one report, 
% each set within that is a subplot, each set within that is the signals to put on each subplot
plotYSigs = {
             {{'LeftDTCmd', 'RightDTCmd'}},
             {{'CompressorCurrent'}, {'PneumaticPress'}},
            };


%filename = 'B:\Projects\RobotCasserole\2016\RoboRIO_data_captures\Saturday_Midwest\log_2016-04-01_160015.csv';
[fname, pathname] = uigetfile('*.csv');

filename = strcat(pathname, fname);

%[garbage1, fname, garbage2] = fileparts(filename);

% Read in the first two lines, which will be the signal name and units values.
disp('Reading headers...');
fp = fopen(filename);
signal_names = strsplit(fgets(fp), ',');
units = strsplit(fgets(fp), ',');
fclose(fp);

% Read the remainder of the data
disp('Reading data...');
data = csvread(filename);


xaxisIndex = getSignalIndex(plotXSig, signal_names);


% Plot all reports
for report_idx = 1:1:length(plotYSigs)

  %Generate new plot window (one per report)
  disp(strcat('Generating report #',num2str(report_idx)));
  figure;
  set(gcf,'name',strcat(fname,'_part_',num2str(report_idx)),'numbertitle','off'); 
  subplot(length(plotYSigs{report_idx}),1,1);
  
  
  for subplot_idx = 1:1:length(plotYSigs{report_idx})
    %Switch to the proper subplot
    subplot(length(plotYSigs{report_idx}),1,subplot_idx);
    
    hold on;
    grid on;
    for sig_idx = 1:1:length(plotYSigs{report_idx}{subplot_idx})
      %For the subplot, plot the correct signals
      index = getSignalIndex(plotYSigs{report_idx}{subplot_idx}{sig_idx}, signal_names);
      if(index == -1)
        warning(strcat('Cannot find signal named ', plotYSigs{report_idx}{subplot_idx}{sig_idx}));
      else
        plot(data(3:end,xaxisIndex), data(3:end,index),getLineSpec(sig_idx));
      end
      
    endfor
    legend(plotYSigs{report_idx}{subplot_idx}{:});
    xlabel(strcat(plotXSig,'(',units{1,xaxisIndex},')'));
    ylabel(units{1,index});
    hold off;
  
  endfor
  


endfor