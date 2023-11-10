import 'package:flutter/cupertino.dart';
import 'package:provider/provider.dart';
import 'app_data.dart';

class WidgetSelectableList extends StatefulWidget {
  const WidgetSelectableList({super.key});

  @override
  WidgetSelectableListState createState() => WidgetSelectableListState();
}

class WidgetSelectableListState extends State<WidgetSelectableList> {
  @override
  Widget build(BuildContext context) {
    AppData appData = Provider.of<AppData>(context);
    return ListView.builder(
      itemCount: appData.clients.length,
      itemBuilder: (context, index) {
        final client = appData.clients[index];
        return GestureDetector(
          onTap: () {
            setState(() {
              appData.selectClient(index);
            });
          },
          child: Container(
            color: appData.selectedClientIndex == index
                ? const Color.fromARGB(255, 20, 181, 255)
                : null,
            child: Text(client,
                style: TextStyle(
                    color: appData.selectedClientIndex == index
                        ? const Color.fromARGB(255, 255, 255, 255)
                        : null,
                    fontSize: 16,
                    fontWeight: FontWeight.w400)),
          ),
        );
      },
    );
  }
}
