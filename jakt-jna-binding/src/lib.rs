extern crate libc;
extern crate core;

use std::ffi::{CStr, CString};
use serde_derive::Serialize;
use libc::c_char;
use jakt::{
    JaktError,
    lexer,
    parser,
    compiler::Compiler,
    typechecker::{Project, typecheck_namespace_declarations},
};
use jakt::compiler::check_codegen_preconditions;
use jakt::typechecker::typecheck_namespace_predecl;

#[derive(Serialize)]
enum TypecheckResult {
    ParseError(JaktError),
    TypeError(Project, JaktError),
    Ok(Project),
}

fn typecheck_result(bytes: &[u8]) -> TypecheckResult {
    let mut project = Project::new();
    let mut compiler = Compiler::new(vec![]);
    if let Some(_err) = compiler.include_prelude(&mut project) {
        panic!("Failed to include prelude")
    }

    let (tokens, error) = lexer::lex(0, bytes);
    if let Some(error) = error {
        return TypecheckResult::ParseError(error)
    };

    let (namespace, error) = parser::parse_namespace(tokens.as_slice(), &mut 0, &mut compiler);
    if let Some(error) = error {
        return TypecheckResult::ParseError(error)
    }

    if let Some(error) = typecheck_namespace_predecl(&namespace, 0, &mut project) {
        return TypecheckResult::TypeError(project, error)
    }

    if let Some(error) = typecheck_namespace_declarations(&namespace, 0, &mut project) {
        return TypecheckResult::TypeError(project, error)
    }

    if let Some(error) = check_codegen_preconditions(&project) {
        return TypecheckResult::TypeError(project, error)
    }

    TypecheckResult::Ok(project)
}

#[no_mangle]
pub fn typecheck(string: *const c_char) -> *mut c_char {
    let string = unsafe {
        assert!(!string.is_null());
        CStr::from_ptr(string)
    };

    let result = typecheck_result(string.to_bytes());
    let serialized_result = serde_json::to_string(&result).unwrap();
    CString::new(serialized_result).unwrap().into_raw()
}
