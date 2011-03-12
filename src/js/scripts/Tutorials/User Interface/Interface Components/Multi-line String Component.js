////////////////////////////////////////////////////////////////////////////////
// This script belongs to the following tutorial:
//
// http://scriptographer.org/tutorials/user-interface/interface-components/

var components = {
	string: {
		type: 'string', label: 'String',
		value: 'This is a string\nwith multiple lines.',
		rows: 6, columns: 30
	}
};

var palette = new Palette('Multi-line String Component', components);
