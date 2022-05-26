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
    typechecker::{Project, typecheck_namespace},
};

#[derive(Serialize)]
enum TypecheckResult {
    ParseError(JaktError),
    TypeError(JaktError),
    Ok(Project),
}

fn typecheck_result(bytes: &[u8]) -> TypecheckResult {
    let mut project = Project::new();
    if let Some(_err) = Compiler::new().include_prelude(&mut project) {
        panic!("Failed to include prelude")
    }

    let (tokens, error) = lexer::lex(0, bytes);
    if let Some(error) = error {
        return TypecheckResult::ParseError(error)
    };

    let (namespace, error) = parser::parse_namespace(tokens.as_slice(), &mut 0);
    if let Some(error) = error {
        return TypecheckResult::ParseError(error)
    }

    match typecheck_namespace(&namespace, 0, &mut project) {
        Some(error) => TypecheckResult::TypeError(error),
        _ => TypecheckResult::Ok(project)
    }
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
