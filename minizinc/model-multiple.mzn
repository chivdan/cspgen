% MZN file model for IEC 61499 function block ECC induction from scenarios
% Author: Daniil Chivilikhin (chivdan@rain.ifmo.ru)

% given constants and domains

int: C; % machine size
int: V; % scenarios tree size
int: E; % input events count
int: X; % unique input variables sets count
int: O; % output events count
int: L; % output variables count

set of int: colors = 1..C+1;
set of int: vertices = 1..V;
set of int: input_events = 1..E+1;
set of int: guards = 1..X;
set of int: output_events = 1..O+1;
set of int: output_variables = 1..L;
set of int: zero_one = 0..1;

%% given data

array [vertices] of 1..E+1: tree_input_event;
array [vertices] of 1..X: tree_guards;
array [vertices] of output_events: tree_output_event;
array [vertices] of vertices: tree_parent;
array [vertices, output_variables] of zero_one: tree_z;

%% variables

array [vertices] of var colors: x; % vertices colors
array [colors, input_events, guards] of var colors: y; % transitions destinations
array [colors] of var output_events: o; % output events 
array [colors, output_variables] of var zero_one: d_0; % what to do with zero
array [colors, output_variables] of var zero_one: d_1; % what to do with one 

%% constraints

% start vertex corresponds to start state
constraint x[1] = 1;

constraint forall (j in output_variables) (
    d_0[1, j] = 0 
);

constraint forall (j in output_variables) (
    d_1[1, j] = 1 
);

% output event constraints
constraint forall (v in 2..V) ( 
    (x[v] != 1) -> (o[x[v]] = tree_output_event[v])
);

constraint o[1] = O+1;
constraint o[C+1] = O+1;

constraint forall (v in 2..V) (
   x[v] != C+1
);


constraint forall (c in 1..C+1) (
   o[c] = O+1 -> forall (j in output_variables) (d_0[c, j] = 0 /\ d_1[c, j] = 1)
);

% transitions constraints 
constraint forall (v in 2..V where tree_input_event[v] != E+1) (
    y[x[tree_parent[v]], tree_input_event[v], tree_guards[v]] = x[v] 
    /\ (forall (gg in guards) (y[x[tree_parent[v]], E+1, gg] = C+1))
);

constraint forall (v in 2..V where tree_input_event[v] == E+1) (    
    forall (gg in guards) (y[x[tree_parent[v]], E+1, gg] = x[v])  
    /\ forall (e in 1..E, gg in guards) (y[x[tree_parent[v]], e, gg] = C+1)
);

%constraint forall (c in colors, e in 1..E, g in guards) (
%   y[c, e, g] != C+1 -> forall (gg in guards) (y[c, E+1, gg] = C+1)
%);

%at least one transition from initial state exists
%constraint exists (e in 1..E, g in guards) (
%    y[1, e, g] != 1
%);


% redundant constraints to unify y variables
% used to find all solutions
constraint forall (c in 1..C, e in 1..E+1, g in guards) (
    ((not exists (v in 2..V) (tree_input_event[v] == e /\ tree_guards[v] == g /\ x[tree_parent[v]] == c)) /\  (not exists (v in 2..V) (tree_input_event[v] == E+1 /\ x[tree_parent[v]] == c))) -> y[c, e, g] = C+1
);

%constraint forall (c in 1..C) (
%    (not exists (v in 2..V) (tree_input_event[v] == E+1 /\ x[tree_parent[v]] == c)) -> forall (gg in guards) (y[c, E+1, gg] = C+1)
%);


% output algorithms constraints
constraint forall (v in 2..V, j in output_variables) (
    (tree_z[tree_parent[v], j] == 0) -> (d_0[x[v], j] = tree_z[v, j])
);

constraint forall (v in 2..V, j in output_variables) (
    (tree_z[tree_parent[v], j] == 1) -> (d_1[x[v], j] = tree_z[v, j])
);



solve satisfy;                          

output ["# x = ", show(x), "\n",
        "# y = ", show(y), "\n",
        "# o = ", show(o), "\n",
        "# d_0 = ", show(d_0), "\n", 
        "# d_1 = ", show(d_1), "\n"];
