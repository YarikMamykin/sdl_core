function! TagIt()
  silent !clear
  silent !rm -frv cscope* tags .clang_complete
  silent !echo "Building tags and database..."
  silent !find . -name "*.cc" -or -name "*.h" > all.files
  silent !find ../sdl_core_build/src -name "*.cc" -or -name "*.h" >> all.files
  silent !sed -i '/.*3rd_party.*/d' all.files
  silent !sed '/.*test.*\|.*mock.*/d' all.files | tee src.files
  silent !cat all.files | grep '.*test.*\|.*mock.*' | tee test.files
  silent !cp all.files .clang_complete
  silent !cscope -b -k -q -u -i src.files -f src_cscope.out
  silent !cscope -b -k -q -u -i test.files -f test_cscope.out
  silent !cat .clang_complete | ctags --sort=yes --c++-kinds=+p --fields=+iaS --extras=+q --language-force=C++ -f tags -L -
  silent !sed -i 's/^/-I/g' .clang_complete
  silent !echo "Connecting database..."
  silent cs kill -1
  silent cs add src_cscope.out
  silent cs add test_cscope.out
  silent !echo "Done"
  redraw!
endfunction

" add src connection
nnoremap <Leader>CAS :cs add src_cscope.out<CR>
" add test connection
nnoremap <Leader>CAT :cs add test_cscope.out<CR>

command! CheckStyle :!./tools/infrastructure/check_style.sh --fix
