function spec = getLineSpec(index)

  avail_specs = {
                   'b',
                   'r',
                   'g',
                   'm',
                   'c',
                   'y',
                   'k',
                   'b:',
                   'r:',
                   'g:',
                   'm:',
                   'c:',
                   'y:',
                   'k:',
                };
                
    spec = avail_specs{mod((index-1), size(avail_specs))+1};

endfunction