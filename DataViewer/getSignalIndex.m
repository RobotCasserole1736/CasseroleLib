function index = getSignalIndex(des_signal, signal_names)

  % simple linear search through "signal names" for the desired name
  for i = 1:1:length(signal_names)
    if(strcmp(strtrim(signal_names{1,i}), strtrim(des_signal)) == 1)
      index = i;
      return;
    endif
  endfor
   
   %if not found, return -1
  index = -1;
  

endfunction